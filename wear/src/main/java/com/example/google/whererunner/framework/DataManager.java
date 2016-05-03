package com.example.google.whererunner.framework;


import com.firebase.client.Firebase;

public class DataManager {

    private static DataManager mDataManager = new DataManager();

    //private Map<String, Map<String, String>> mDataPoints = new HashMap<>();
    private Firebase mFirebaseRef;

    private DataManager(){
        mFirebaseRef = new Firebase("https://where-runner.firebaseio.com/");
    }

    public static DataManager getInstance(){
        return mDataManager;
    }

    public void saveDataPoint(String collection, String value){
        mFirebaseRef.child(collection).setValue(value);
    }

}
