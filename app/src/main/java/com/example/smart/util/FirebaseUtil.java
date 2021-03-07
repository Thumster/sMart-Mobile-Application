package com.example.smart.util;

import android.util.Log;

import com.example.smart.model.Item;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public abstract class FirebaseUtil {

    private static FirebaseFirestore FIRESTORE;

    public static FirebaseFirestore getFirestore() {
        if (FIRESTORE == null) {
            FIRESTORE = FirebaseFirestore.getInstance();
            Log.w("FIREBASE", "No Firestore instance detected, created new instance...");
        } else {
            Log.w("FIREBASE", "Firestore instance detected, returning old instance");
        }

        return FIRESTORE;
    }

}
