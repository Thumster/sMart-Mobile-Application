package com.example.smart.util;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.smart.model.CartItem;
import com.example.smart.ui.home.QRDialogFragment;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class FirebaseUtil {

    private static final String TAG = "FIREBASEUTIL";

    public static final String ITEM_COLLECTION_NAME = "items";
    public static final String USER_COLLECTION_NAME = "users";
    public static final String CART_COLLECTION_NAME = "carts";
    public static final String CART_USER_DOC_NAME = "user";
    public static final String USER_CART_ITEMS_COLLECTION_NAME = "cartItems";

    private static Boolean isCurrentlyShopping = false;
    private static String currentUserCartId;

    private static FirebaseFirestore FIRESTORE;
    private static FirebaseAuth AUTH;

    private static OnCartFound onCartFoundListener;

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

    public interface OnCartFound {
        void onCartFound(Boolean found);
    }

    public static void startListening(OnCartFound listener) {
        onCartFoundListener = listener;
    }

    private static void updateOnCartFoundListener() {
        if (onCartFoundListener != null) {
            onCartFoundListener.onCartFound(getIsCurrentlyShopping());
        }
    }

    public static void initCart() {
        // =============================== CART INIT ===============================================
        Query checkUserCart = getCartsRef().whereEqualTo(CART_USER_DOC_NAME, getCurrentUserUid());
        checkUserCart.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> docs = task.getResult().getDocuments();
                if (docs.size() > 0) {
                    String cartId = docs.get(0).getId();
                    if (docs.size() > 1) {
                        Log.i(TAG, "User is tagged to multiple carts, check this error in db");
                        Log.i(TAG, "Tagging user to first cart instead: " + cartId);
                    } else {
                        Log.i(TAG, "User is currently tagged to a cart: " + cartId);
                    }
                    FirebaseUtil.setIsCurrentlyShopping(true);
                    FirebaseUtil.setCurrentUserCartId(cartId);
                } else {
                    FirebaseUtil.setIsCurrentlyShopping(false);
                    Log.i(TAG, "No cart found to be tagged to user");
                }

            } else {
                FirebaseUtil.setIsCurrentlyShopping(false);
                Log.e(TAG, "firestore get failed with ", task.getException());
            }
            updateOnCartFoundListener();
        });
    }

    //==============================================================================================
    //================================= GETTERS/SETTERS ============================================
    //==============================================================================================

    public static String getCurrentUserCartId() {
        return FirebaseUtil.currentUserCartId;
    }

    public static void setCurrentUserCartId(String cartId) {
        FirebaseUtil.currentUserCartId = cartId;
    }

    public static String getCurrentUserUid() {
        return getAuth().getCurrentUser().getUid();
    }

    public static Boolean getIsCurrentlyShopping() {
        return FirebaseUtil.isCurrentlyShopping;
    }

    public static void setIsCurrentlyShopping(Boolean isCurrentlyShopping) {
        if (!isCurrentlyShopping) {
            FirebaseUtil.setCurrentUserCartId(null);
        }
        FirebaseUtil.isCurrentlyShopping = isCurrentlyShopping;
    }

    //==============================================================================================
    //================================= COLLECTION CALLS ===========================================
    //==============================================================================================

    public static CollectionReference getItemsRef() {
        return getFirestore().collection(ITEM_COLLECTION_NAME);
    }

    public static CollectionReference getCartsRef() {
        return getFirestore().collection(CART_COLLECTION_NAME);
    }

    public static CollectionReference getUserCartItemsRef() {
        return getFirestore().collection(USER_COLLECTION_NAME).document(getCurrentUserUid()).collection(USER_CART_ITEMS_COLLECTION_NAME);
    }

}
