package com.example.wcbsettings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import static com.example.wcbsettings.MyNFCTag.*;
import static com.example.wcbsettings.TagDiscovery.TAG;

public class MainActivity extends AppCompatActivity implements TagDiscovery.onTagDiscoveryCompletedListener{
    private NfcAdapter mNfcAdapter;
    private NFCTag mNfcTag;
    private ST25DVTag mST25DVTag;
    private MyNFCTag mMyNFCTag;

    private EditText mCurrentRatingEdit;
    private EditText mTagIdEdit;
    private EditText mReconnectPeriodEdit;

    private Switch mInitialStateSwitch;
    private Switch mAutoReconnectSwitch;
    private Switch mRandomStartSwitch;
    private EditText mOwnerNameEdit;
    private Button mReadMemoryBtn;
    private Button mWriteMemoryBtn;

    private Vibrator vibrator;

    private View uidLayout;
    private View scanLayout;

    private String currentRatingTemp;
    private String tagIdTemp;
    private String reconnectPeriodTemp;
    private String ownerTemp;

    private boolean writeMode = false;

    static final int RF_CONFIG_PASSWORD = 0;
    static final int EH_MODE = 2;

    public void exitApp(View view) { finish();
    }

    public void enableNFC() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivity(intent);
        } else {
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent);
        }
    }

    enum Action {
        ENABLE_EH,
        READ_TAG_MEMORY,
        WRITE_TAG_MEMORY
    }

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD
    }

    private LinearLayout mHeaderLayout;
    private TextView mHeader;

    int backgroundLevel = 10000;
    Handler handler = new Handler();

    Runnable rUpdateLevel = new Runnable() {
        @Override
        public void run() {
            if(backgroundLevel == 0) {
                writeMode = false;
                backgroundLevel = 10000;
                updateNFCStatus();
            } else {
                updateLevel();
                handler.postDelayed(rUpdateLevel, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mMyNFCTag = MyNFCTag.getInstance();

        uidLayout = findViewById(R.id.uidLayout);
        scanLayout = findViewById(R.id.scanLayout);

        mHeaderLayout = (LinearLayout) findViewById(R.id.headerLayout);
        mHeader = (TextView) findViewById(R.id.header);

        mCurrentRatingEdit = (EditText) findViewById(R.id.currentRatingEdit);
        mCurrentRatingEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    currentRatingTemp = mCurrentRatingEdit.getText().toString();
                    mCurrentRatingEdit.getText().clear();
                } else {
                    if(TextUtils.isEmpty(mCurrentRatingEdit.getText().toString())) {
                        mCurrentRatingEdit.setText(currentRatingTemp);
                    }
                    currentRatingTemp = mCurrentRatingEdit.getText().toString();
                }
            }
        });
        mTagIdEdit = (EditText) findViewById(R.id.tagIdEdit);
        mTagIdEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    tagIdTemp = mTagIdEdit.getText().toString();
                    mTagIdEdit.getText().clear();
                } else {
                    if(TextUtils.isEmpty(mTagIdEdit.getText().toString())) {
                        mTagIdEdit.setText(tagIdTemp);
                    }
                    tagIdTemp = mTagIdEdit.getText().toString();
                }
            }
        });
        mReconnectPeriodEdit = (EditText) findViewById(R.id.reconnectPeriodEdit);
        mReconnectPeriodEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    reconnectPeriodTemp = mReconnectPeriodEdit.getText().toString();
                    mReconnectPeriodEdit.getText().clear();
                } else {
                    if(TextUtils.isEmpty(mReconnectPeriodEdit.getText().toString())) {
                        mReconnectPeriodEdit.setText(reconnectPeriodTemp);
                    }
                    reconnectPeriodTemp = mReconnectPeriodEdit.getText().toString();
                }
            }
        });
        mInitialStateSwitch = (Switch) findViewById(R.id.onOffSwitch);
        mInitialStateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    mMyNFCTag.setInitialState(INITIAL_STATE_BIT);
                } else {
                    mMyNFCTag.setInitialState(0);
                }
            }
        });

        mAutoReconnectSwitch = (Switch) findViewById(R.id.autoReconnectSwitch);
        mAutoReconnectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mMyNFCTag.setAutoReconnect(AUTO_RECONNECT_BIT);
                } else {
                    mMyNFCTag.setAutoReconnect(0);
                }
            }
        });

        mRandomStartSwitch = (Switch) findViewById(R.id.randomStartSwitch);
        mRandomStartSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mMyNFCTag.setRandomStart(RANDOM_START_BIT);
                } else {
                    mMyNFCTag.setRandomStart(0);
                }
            }
        });

        mOwnerNameEdit = (EditText) findViewById(R.id.ownerNameEdit);
        mOwnerNameEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    ownerTemp = mOwnerNameEdit.getText().toString();
                    mOwnerNameEdit.getText().clear();
                } else {
                    if(TextUtils.isEmpty(mOwnerNameEdit.getText().toString())) {
                        mOwnerNameEdit.setText(ownerTemp);
                    }
                    ownerTemp = mOwnerNameEdit.getText().toString();
                }
            }
        });

        mReadMemoryBtn = findViewById(R.id.readMemoryBtn);
        mReadMemoryBtn.setOnClickListener(view ->  {
            writeMode = false;
            handler.removeCallbacks(rUpdateLevel);
            backgroundLevel = 10000;
            mHeader.setText("Reading Tag Mode");
            mHeader.setBackground(getDrawable(R.drawable.ic_title_with_background_color_green));
            if(mNfcTag != null) {
//                clearErrorAlert();
                executeAsynchronousAction(Action.READ_TAG_MEMORY);
            } else {
                Toast.makeText(this, "Action failed!", Toast.LENGTH_SHORT).show();
            }
        });

        mWriteMemoryBtn = findViewById(R.id.writeMemoryBtn);
        mWriteMemoryBtn.setOnClickListener(view ->  {
            if(writeMode) {
                handler.removeCallbacks(rUpdateLevel);
                backgroundLevel = 10000;
            } else {
                writeMode = true;
            }
            updateNFCStatus();
            handler.post(rUpdateLevel);
            if(mNfcTag != null) {
//                checkEditTextEmpty();
                if(mCurrentRatingEdit.length() == 0 || mTagIdEdit.length() == 0 || mReconnectPeriodEdit.length() == 0) {
                    Toast.makeText(MainActivity.this, "There are empty fields", Toast.LENGTH_SHORT).show();
                } else {
//                    showWriteTagDialog();
                    executeAsynchronousAction(Action.WRITE_TAG_MEMORY);
                }
            } else {
                Toast.makeText(this, "Action failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if if this phone has NFC hardware
        if (mNfcAdapter == null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            // set title
            alertDialogBuilder.setTitle("Warning!");
            // set dialog message
            alertDialogBuilder
                    .setMessage("This phone doesn't have NFC hardware!")
                    .setCancelable(true)
                    .setPositiveButton("Leave", (dialog, id) -> {
                        dialog.cancel();
                        finish();
                    });
            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            // show it
            alertDialog.show();
        } else {
            PendingIntent pendingIntent;
            //Toast.makeText(this, "We are ready to play with NFC!", Toast.LENGTH_SHORT).show();
            // Give priority to the current activity when receiving NFC events (over other actvities)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            }
            IntentFilter[] nfcFilters = null;
            String[][] nfcTechLists = null;
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcFilters, nfcTechLists);
        }
        // The current activity can be resumed for several reasons (NFC tag tapped is one of them).
        // Check what was the reason which triggered the resume of current application
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED) || action.equals(NfcAdapter.ACTION_TECH_DISCOVERED) || action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            // If the resume was triggered by an NFC event, it will contain an EXTRA_TAG providing
            // the handle of the NFC Tag
            Tag androidTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (androidTag != null) {
                // This action will be done in an Asynchronous task.
                // onTagDiscoveryCompleted() of current activity will be called when the discovery is completed.
                new TagDiscovery(this).execute(androidTag);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_eeprom);
        menuItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.shut_down:
                exit();
                return true;
            case R.id.menu_details:
                Intent detailsIntent = new Intent(this, DetailsActivity.class);
                detailsIntent.setAction(Intent.ACTION_DEFAULT);
                startActivity(detailsIntent);
                return true;
            case android.R.id.home:
                uidLayout.setVisibility(View.GONE);
                scanLayout.setVisibility(View.VISIBLE);
                return true;
            case R.id.nfc:
                enableNFC();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void exit() {
        // Use the Builder class for convenient dialog construction
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("Shut Down Apps?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAffinity();
                System.exit(0);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // onResume() gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public void onTagDiscoveryCompleted(NFCTag nfcTag, TagHelper.ProductID productId, STException error) {
        if (error != null) {
            Toast.makeText(getApplication(), "Error while reading the tag: " + error.toString(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (nfcTag != null) {
            mNfcTag = nfcTag;
            mST25DVTag = (ST25DVTag) mNfcTag;
            try {
                String uidString = nfcTag.getUidString();
                uidLayout.setVisibility(View.VISIBLE);
                scanLayout.setVisibility(View.GONE);
                executeAsynchronousAction(Action.ENABLE_EH);
                if(writeMode) {
                    executeAsynchronousAction(Action.WRITE_TAG_MEMORY);
                } else {
                    mHeader.setText("Reading Tag Mode");
                    mHeader.setBackground(getDrawable(R.drawable.ic_title_with_background_color_green));
                    executeAsynchronousAction(Action.READ_TAG_MEMORY);
                }
            } catch (STException e) {
                e.printStackTrace();
//                Toast.makeText(this, "Discovery successful but failed to read the tag!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Tag discovery failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
        new myAsyncTask(action).execute();
    }

    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;
        byte[] data;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;

            try {
                switch (mAction) {
                    case ENABLE_EH:
                        enableEH();
                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    case READ_TAG_MEMORY:
                        data = mNfcTag.readBytes(MyNFCTag.DATA_START, MyNFCTag.DATA_LENGTH);
                        if(data.length == MyNFCTag.DATA_LENGTH) {
                            mMyNFCTag.setRawData(data);
                            mMyNFCTag.readTagData();
                        }
                        // If we get to this point, it means that no STException occured so the action was successful
                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    case WRITE_TAG_MEMORY:
                        if(TextUtils.isEmpty(mCurrentRatingEdit.getText().toString()) || TextUtils.isEmpty(mTagIdEdit.getText().toString()) ||
                                TextUtils.isEmpty(mReconnectPeriodEdit.getText().toString())) {
                        return ActionStatus.ACTION_FAILED;
                        }
                        mMyNFCTag.setCurrentDouble(Double.parseDouble(mCurrentRatingEdit.getText().toString()));
                        mMyNFCTag.setTagId(Integer.parseInt(mTagIdEdit.getText().toString()));
                        mMyNFCTag.setReconnectPeriod(Integer.parseInt(mReconnectPeriodEdit.getText().toString()));
                        mMyNFCTag.setOwnerName(mOwnerNameEdit.getText().toString());
                        if(mST25DVTag.isMailboxEnabled(true)) {
                            mST25DVTag.disableMailbox();
                        }
                        mNfcTag.writeBytes(MyNFCTag.DATA_START, mMyNFCTag.writeTagData());
                        mST25DVTag.enableMailbox();
                        // If we get to this point, it means that no STException occured so the action was successful
                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    default:
                        result = ActionStatus.ACTION_FAILED;
                        break;
                }

            } catch (STException e) {
                if (e.getError() == STException.STExceptionCode.TAG_NOT_IN_THE_FIELD) {
                    result = ActionStatus.TAG_NOT_IN_THE_FIELD;
                } else {
                    e.printStackTrace();
                    result = ActionStatus.ACTION_FAILED;
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {
            vibrator.vibrate(100);
            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case ENABLE_EH:
//                            Toast.makeText(MainActivity.this, "EH Mode Enabled", Toast.LENGTH_SHORT).show();
                            break;
                        case READ_TAG_MEMORY:
                            data = mMyNFCTag.getRawData();
                            mCurrentRatingEdit.setText(String.valueOf(mMyNFCTag.getCurrentDouble()).trim());
                            mTagIdEdit.setText(String.valueOf(mMyNFCTag.getTagId()).trim());
                            mReconnectPeriodEdit.setText(String.valueOf(mMyNFCTag.getReconnectPeriod()).trim());
                            onOffStatus(mMyNFCTag.getInitialState());
                            autoReconnectStatus(mMyNFCTag.getAutoReconnect());
                            randomStartStatus(mMyNFCTag.getRandomStart());
                            mOwnerNameEdit.setText(String.valueOf(mMyNFCTag.getOwnerName()).trim());
                            Toast.makeText(MainActivity.this, "Read successful", Toast.LENGTH_SHORT).show();
                            break;
                        case WRITE_TAG_MEMORY:
                            writeMode = false;
                            handler.removeCallbacks(rUpdateLevel);
                            backgroundLevel = 10000;
                            updateNFCStatus();
                            Toast.makeText(MainActivity.this, "Write successful", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;

                case ACTION_FAILED:
                    Toast.makeText(MainActivity.this, "Action failed!", Toast.LENGTH_SHORT).show();
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    Toast.makeText(MainActivity.this, "Tag not in the field!", Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    }

    private void onOffStatus(int status) {
        if(status == INITIAL_STATE_BIT) {
            mInitialStateSwitch.setChecked(true);
        } else {
            mInitialStateSwitch.setChecked(false);
        }
    }

    private void autoReconnectStatus(int status) {
        if(status == AUTO_RECONNECT_BIT) {
            mAutoReconnectSwitch.setChecked(true);
        } else {
            mAutoReconnectSwitch.setChecked(false);
        }
    }

    private void randomStartStatus(int status) {
        if(status == RANDOM_START_BIT) {
            mRandomStartSwitch.setChecked(true);
        } else {
            mRandomStartSwitch.setChecked(false);
        }
    }

    public void enableEH() {
        byte[] mPassword = {0,0,0,0,0,0,0,0};
        byte enable_EH = 0;
        byte[] read_EHConfig;
        try {
            read_EHConfig = mST25DVTag.readConfig(EH_MODE);
            if(read_EHConfig[1] != 0) {
                mST25DVTag.presentPassword(RF_CONFIG_PASSWORD, mPassword);
                mST25DVTag.writeConfig(EH_MODE, enable_EH);
            }

        } catch (STException e) {
            e.printStackTrace();
        }
    }

    public void updateNFCStatus() {
        if(writeMode) {
            mHeader.setText("Writing Tag Mode");
            mHeader.setBackground(getDrawable(R.drawable.level));
        } else {
            mHeader.setBackground(null);
        }
    }

    public void updateLevel() {
        mHeader.getBackground().setLevel(backgroundLevel);
        backgroundLevel-= 1000;
    }

//    public void showWriteTagDialog() {
//        Dialog dialog = new Dialog(this);
//        dialog.setContentView(R.layout.dialog_fragment_writetag);
//        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        writeMode = false;
//                    }
//                });
//        EditText dialogTagIdEdit = (EditText) dialog.findViewById(R.id.dialogTagIdEdit);
//        EditText dialogCurrentRatingEdit = (EditText) dialog.findViewById(R.id.dialogCurrentRatingEdit);
//        EditText dialogReconnectPeriodEdit = (EditText) dialog.findViewById(R.id.dialogReconnectPeriodEdit);
//        Switch dialogOnOffSwitch = (Switch) dialog.findViewById(R.id.dialogOnOffSwitch);
//        Switch dialogAutoReconnectSwitch = (Switch) dialog.findViewById(R.id.dialogAutoReconnectSwitch);
//        Switch dialogRandomStartSwitch = (Switch) dialog.findViewById(R.id.dialogRandomStartSwitch);
//        EditText dialogOwnerNameEdit = (EditText) dialog.findViewById(R.id.dialogOwnerNameEdit);
//        dialogTagIdEdit.setText(mTagIdEdit.getText());
//        dialogCurrentRatingEdit.setText(mCurrentRatingEdit.getText());
//        dialogReconnectPeriodEdit.setText(mReconnectPeriodEdit.getText());
//        dialogOnOffSwitch.setChecked(mInitialStateSwitch.isChecked());
//        dialogAutoReconnectSwitch.setChecked(mAutoReconnectSwitch.isChecked());
//        dialogRandomStartSwitch.setChecked(mRandomStartSwitch.isChecked());
//        dialogOwnerNameEdit.setText(mOwnerNameEdit.getText());
//        writeMode = true;
//        dialog.show();
//    }

    /*public void checkEditTextEmpty() {
        if(mCurrentRatingEdit.length() == 0) {
            mCurrentRatingWarning.setVisibility(View.VISIBLE);
        } else {
            mCurrentRatingWarning.setVisibility(View.INVISIBLE);
            mMyNFCTag.setCurrentDouble(Double.parseDouble(mCurrentRatingEdit.getText().toString()));
        }
        if(mTagIdEdit.length() == 0) {
            mTagIdWarning.setVisibility(View.VISIBLE);
        } else {
            mTagIdWarning.setVisibility(View.INVISIBLE);
            mMyNFCTag.setTagId(Integer.parseInt(mTagIdEdit.getText().toString()));
        }
        if(mReconnectPeriodEdit.length() == 0) {
            mReconnectPeriodWarning.setVisibility(View.VISIBLE);
        } else {
            mReconnectPeriodWarning.setVisibility(View.INVISIBLE);
            mMyNFCTag.setReconnectPeriod(Integer.parseInt(mReconnectPeriodEdit.getText().toString()));
        }
    }*/

 /*   public void clearErrorAlert() {
        mCurrentRatingWarning.setVisibility(View.INVISIBLE);
        mTagIdWarning.setVisibility(View.INVISIBLE);
        mReconnectPeriodWarning.setVisibility(View.INVISIBLE);
    }*/

}