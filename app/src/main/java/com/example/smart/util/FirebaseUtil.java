package com.example.smart.util;

import android.util.Log;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class FirebaseUtil {

    private static final String TAG = "FIREBASEUTIL";

    private static final String ITEM_COLLECTION_NAME = "items";
    private static final String USER_COLLECTION_NAME = "users";
    private static final String CART_COLLECTION_NAME = "cart";

    private static FirebaseFirestore FIRESTORE;
    private static FirebaseAuth AUTH;
    private static AuthUI AUTH_UI;

    public static FirebaseFirestore getFirestore() {
        if (FIRESTORE == null) {
            FIRESTORE = FirebaseFirestore.getInstance();
            Log.i(TAG, "No Firestore instance detected, created new instance...");
        } else {
            Log.i(TAG, "Firestore instance detected, returning old instance");
        }

        return FIRESTORE;
    }

    public static FirebaseAuth getAuth() {
        if (AUTH == null) {
            AUTH = FirebaseAuth.getInstance();
        }

        return AUTH;
    }

    public static Boolean isSignedIn() {
        Boolean isSignedIn = getAuth().getCurrentUser() != null;
        if (isSignedIn) {
            Log.i(TAG, "User is currently - SIGNED IN");
        } else {
            Log.i(TAG, "User is currently - NOT SIGNED IN");
        }
        return isSignedIn;
    }

    public static FirebaseUser getCurrentUser() {
        FirebaseUser user = getAuth().getCurrentUser();
        if (user == null) {
            Log.i(TAG, "No user detected");
        } else {
            Log.i(TAG, "Retrieving Firebase User"
                    + "\n\tUID:\t" + user.getUid()
                    + "\n\tDisplay Name:\t" + user.getDisplayName());
        }
        return user;
    }

    public static String getCurrentUserUid() {
        return getAuth().getCurrentUser().getUid();
    }

    public static AuthUI getAuthUI() {
        if (AUTH_UI == null) {
            AUTH_UI = AuthUI.getInstance();
        }

        return AUTH_UI;
    }

    public static CollectionReference getItemsRef() {
        return getFirestore().collection(ITEM_COLLECTION_NAME);
    }

    public static CollectionReference getUserCartRef() {
        return getFirestore().collection(USER_COLLECTION_NAME).document(getCurrentUserUid()).collection(CART_COLLECTION_NAME);
    }

}
