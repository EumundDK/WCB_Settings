package com.example.wcbsettings;

import java.nio.charset.StandardCharsets;

public class MyNFCTag {

    public static int INITIAL_STATE_BIT = 1;
    public static int AUTO_RECONNECT_BIT = 2;
    public static int RANDOM_START_BIT = 4;

    static final int CURRENT_RATING = 0;
    static final int TAG_ID = 2;
    static final int RECONNECT_PERIOD = 4;
    static final int SETTINGS = 6;
    static final int OWNER_NAME = 8;

    static final int DATA_START = 0;
    static final int OWNER_LENGTH = 16;
    static final int DATA_LENGTH = 24;

    byte[] rawData = new byte[DATA_LENGTH];
    byte[] ownerNameRaw = new byte[OWNER_LENGTH];
    int currentRating;
    int tagId;
    int reconnectPeriod;
    int initialState;
    int autoReconnect;
    int randomStart;
    String ownerName;

    private static final MyNFCTag myNfcTag = new MyNFCTag();

    public static MyNFCTag getInstance() {
        return myNfcTag;
    }

    public MyNFCTag() {

    }

    public int getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(int currentRating) {
        this.currentRating = currentRating;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public int getReconnectPeriod() {
        return reconnectPeriod;
    }

    public void setReconnectPeriod(int reconnectPeriod) {
        this.reconnectPeriod = reconnectPeriod;
    }

    public int getInitialState() {
        return initialState;
    }

    public void setInitialState(int initialState) {
        this.initialState = initialState;
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

    public void readTagData() {
        System.arraycopy(rawData, OWNER_NAME, ownerNameRaw, 0, ownerNameRaw.length);

        currentRating = ((rawData[CURRENT_RATING] & 0xFF) + ((rawData[CURRENT_RATING+1] & 0xFF) * 256));
        tagId = (rawData[TAG_ID] & 0xFF) + ((rawData[TAG_ID+1] & 0xFF) * 256);
        reconnectPeriod = (rawData[RECONNECT_PERIOD] & 0xFF) + ((rawData[RECONNECT_PERIOD +1] & 0xFF) * 256);
        initialState = (rawData[SETTINGS] & INITIAL_STATE_BIT);
        autoReconnect = (rawData[SETTINGS] & AUTO_RECONNECT_BIT);
        randomStart = (rawData[SETTINGS] & RANDOM_START_BIT);
        ownerName = new String(ownerNameRaw, StandardCharsets.UTF_8);
    }

    public byte[] writeTagData() {
        byte[] tempRawData = new byte[DATA_LENGTH];
        byte[] tempOwnerName = new byte[OWNER_LENGTH];

        if(currentRating < 256) {
            tempRawData[CURRENT_RATING] = (byte) currentRating;
            tempRawData[CURRENT_RATING + 1] = 0;
        } else {
            tempRawData[CURRENT_RATING] = (byte) (currentRating % 256);
            tempRawData[CURRENT_RATING + 1] = (byte) (currentRating / 256);
        }
        tempRawData[TAG_ID] = (byte) tagId;
        if(reconnectPeriod < 256) {
            tempRawData[RECONNECT_PERIOD] = (byte) reconnectPeriod;
            tempRawData[RECONNECT_PERIOD + 1] = 0;
        } else {
            tempRawData[RECONNECT_PERIOD] = (byte) (reconnectPeriod % 256);
            tempRawData[RECONNECT_PERIOD + 1] = (byte) (reconnectPeriod / 256);
        }
        tempRawData[SETTINGS] = (byte) (initialState + autoReconnect + randomStart);
        System.arraycopy(ownerName.getBytes(StandardCharsets.US_ASCII), 0, tempOwnerName, 0, ownerName.getBytes(StandardCharsets.US_ASCII).length);
        System.arraycopy(tempOwnerName, 0, tempRawData, OWNER_NAME, tempOwnerName.length);

        return tempRawData;
    }

    public void setCurrentDouble(double currentSetting) {
        this.currentRating = (int) (currentSetting * 10.0);
    }

    public double getCurrentDouble() {
        return (double) currentRating / 10.0;
    }
}
