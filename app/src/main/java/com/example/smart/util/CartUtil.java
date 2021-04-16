package com.example.smart.util;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

// LEAVE THIS CLASS FOR CHECKING OF CART POSITION - currently not used
public class CartUtil
        implements EventListener<QuerySnapshot> {

    private static final String TAG = "CART_UTIL";

    private Query query;
    private ListenerRegistration registration;

    private DocumentSnapshot snapshot;

    public CartUtil(Query query) {
        this.query = query;
    }

    public void startListening() {
        if (query != null && registration == null) {
            registration = query.addSnapshotListener(this);
        }
    }

    public void stopListening() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }

        snapshot = null;
    }

    @Override
    public void onEvent(QuerySnapshot documentSnapshot,
                        FirebaseFirestoreException e) {
        // Dispatch the event
//        for (DocumentChange change : documentSnapshot.getDocumentChanges()) {
//            // Snapshot of the changed document
//            DocumentSnapshot snapshot = change.getDocument();
//
//            switch (change.getType()) {
//                case ADDED:
//                    onDocumentAdded(change); // Add this line
//                    break;
//                case MODIFIED:
//                    onDocumentModified(change); // Add this line
//                    break;
//                case REMOVED:
//                    onDocumentRemoved(change); // Add this line
//                    break;
//            }
//        }

        onDataChanged();
    }

    protected DocumentSnapshot getSnapshot() {
        return snapshot;
    }

    protected void onError(FirebaseFirestoreException e) {
    }

    protected void onDataChanged() {
    }
}
