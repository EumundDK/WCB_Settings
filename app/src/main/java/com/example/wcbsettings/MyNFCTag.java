package com.example.wcbsettings;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MyNFCTag {

    static final int CURRENT_SETTING = 0;
    static final int TAG_ID = 2;
    static final int CUTOFF_PERIOD = 4;
    static final int AUTO_ONOFF_SETTING = 6;
    static final int RANDOM_START = 7;
    static final int OWNER_NAME = 8;

    static final int DATA_START = 0;
    static final int OWNER_LENGTH = 16;
    static final int DATA_LENGTH = 24;

    byte[] rawData = new byte[DATA_LENGTH];
    byte[] ownerNameRaw = new byte[OWNER_LENGTH];
    int currentSetting;
    int tagId;
    int cutOffPeriod;
    int autoOnOffSetting;
    int onOffSetting;
    int autoReconnect;
    int randomStart;
    String ownerName;

    private static final MyNFCTag myNfcTag = new MyNFCTag();

    public static MyNFCTag getInstance() {
        return myNfcTag;
    }

    public MyNFCTag() {

    }

    public int getCurrentSetting() {
        return currentSetting;
    }

    public void setCurrentSetting(int currentSetting) {
        this.currentSetting = currentSetting;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public int getCutOffPeriod() {
        return cutOffPeriod;
    }

    public void setCutOffPeriod(int cutOffPeriod) {
        this.cutOffPeriod = cutOffPeriod;
    }

    public int getOnOffSetting() {
        return onOffSetting;
    }

    public void setOnOffSetting(int onOffSetting) {
        this.onOffSetting = onOffSetting;
    }

    public int getAutoReconnect() {
        return autoReconnect;
    }

    public void setAutoReconnect(int autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public int getRandomStart() {
        return randomStart;
    }

    public void setRandomStart(int randomStart) {
        this.randomStart = randomStart;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public void setData() {
        currentSetting = ((rawData[CURRENT_SETTING] & 0xFF) + ((rawData[CURRENT_SETTING+1] & 0xFF) * 256));
        tagId = (rawData[TAG_ID] & 0xFF) + ((rawData[TAG_ID+1] & 0xFF) * 256);
        cutOffPeriod = (rawData[CUTOFF_PERIOD] & 0xFF) + ((rawData[CUTOFF_PERIOD+1] & 0xFF) * 256);
        autoOnOffSetting = (rawData[AUTO_ONOFF_SETTING] & 0xFF);
        onOffSetting = (rawData[AUTO_ONOFF_SETTING] & 0x01);
        autoReconnect = (rawData[AUTO_ONOFF_SETTING] & 0x02);
        randomStart = (rawData[RANDOM_START] & 0xFF);

/*        for (int i = OWNER_NAME; i < OWNER_NAME + ownerNameRaw.length; i++) {
            if(rawData[i] != 0) {
                ownerNameRaw[i - OWNER_NAME] = rawData[i];
            }
        }*/
        System.arraycopy(rawData, OWNER_NAME, ownerNameRaw, 0, ownerNameRaw.length);
//        System.arraycopy(ownerNameRaw, 0, rawData, OWNER_NAME, ownerNameRaw.length);
        ownerName = new String(ownerNameRaw, StandardCharsets.UTF_8);
    }

    public byte[] getData() {
        byte[] mOwnerName = new byte[OWNER_LENGTH];
        mOwnerName = ownerName.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < OWNER_LENGTH; i++){
            ownerNameRaw[i] = 0;
        }

        System.arraycopy(mOwnerName, 0, ownerNameRaw, 0, mOwnerName.length);

        if(currentSetting < 256) {
            rawData[CURRENT_SETTING] = (byte) currentSetting;
            rawData[CURRENT_SETTING + 1] = 0;
        } else {
            rawData[CURRENT_SETTING] = (byte) (currentSetting % 256);
            rawData[CURRENT_SETTING + 1] = (byte) (currentSetting / 256);
        }
        rawData[TAG_ID] = (byte) tagId;
        if(cutOffPeriod < 256) {
            rawData[CUTOFF_PERIOD] = (byte) cutOffPeriod;
            rawData[CUTOFF_PERIOD + 1] = 0;
        } else {
            rawData[CUTOFF_PERIOD] = (byte) (cutOffPeriod % 256);
            rawData[CUTOFF_PERIOD + 1] = (byte) (cutOffPeriod / 256);
        }
        rawData[AUTO_ONOFF_SETTING] = (byte) (onOffSetting + autoReconnect);
        rawData[RANDOM_START] = (byte) randomStart;

        System.arraycopy(ownerNameRaw, 0, rawData, OWNER_NAME, ownerNameRaw.length);
//        System.arraycopy(rawData, OWNER_NAME, ownerNameRaw, OWNER_LENGTH, ownerNameRaw.length);
//        ownerName = new String(ownerNameRaw, StandardCharsets.UTF_8);


        return rawData;
    }

    public void setCurrentDouble(double currentSetting) {
        this.currentSetting = (int) (currentSetting * 10.0);
    }

    public double getCurrentDouble() {
        return (double) currentSetting / 10.0;
    }
}
