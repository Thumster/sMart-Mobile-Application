package com.example.smart.ui.item;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart.R;
import com.example.smart.adapter.ItemAdapter;
import com.example.smart.model.CartItem;
import com.example.smart.model.Item;
import com.example.smart.util.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

public class ItemsFragment extends Fragment implements
        ItemAdapter.OnItemSelectedListener, ItemDialogFragment.ItemDialogListener {

    private static final String TAG = "ITEMS_FRAGMENT";

    private ItemAdapter itemAdapter;
    private RecyclerView itemsRecycler;
    private ViewGroup emptyView;

    private Query query;
    private CollectionReference itemsColRef;
    private CollectionReference cartColRef;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_items, container, false);
        itemsRecycler = root.findViewById(R.id.recycler_items);
        emptyView = root.findViewById(R.id.view_empty);

        itemsColRef = FirebaseUtil.getItemsRef();
        cartColRef = FirebaseUtil.getUserCartItemsRef();
        query = itemsColRef;
        initRecyclerView(root);

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey("itemId")) {
                String itemId = (String) args.get("itemId");
                Log.i(TAG, "Received item: " + itemId);
                FirebaseUtil.getItemsRef().document(itemId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot.exists()) {
                            onItemSelected(snapshot);
                        }
                    }
                });
            }
        }

        return root;
    }

    private void initRecyclerView(View view) {
        if (query == null) {
            Log.i(TAG, "No query, not initializing items RecyclerView");
        }
        itemAdapter = new ItemAdapter(query, this) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    itemsRecycler.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    itemsRecycler.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
//                // Show a snackbar on errors
                Snackbar.make(view.findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }

        };

        itemsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        itemsRecycler.setAdapter(itemAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start listening for Firestore updates
        if (itemAdapter != null) {
            itemAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (itemAdapter != null) {
            itemAdapter.stopListening();
        }
    }

    @Override
    public void onItemSelected(DocumentSnapshot item) {
        Item itemSelected = item.toObject(Item.class);
        DialogFragment itemDialog = new ItemDialogFragment(this, itemSelected);
        itemDialog.show(getFragmentManager(), TAG);
    }

    @Override
    public void onItemAdded(DocumentSnapshot item) {
        Item itemSelected = item.toObject(Item.class);
        String itemId = itemSelected.getId().getId();

        DocumentReference docRef = cartColRef.document(itemId);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    CartItem cartItem;
                    if (document.exists()) {
                        cartItem = document.toObject(CartItem.class);
                        cartItem.setQuantity(cartItem.getQuantity() + 1);
                    } else {
                        cartItem = new CartItem(itemSelected);
                    }
                    docRef.set(cartItem).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "Successful write to firestore");
                            try {
                                Toast.makeText(getContext(), "Successfully added to cart", Toast.LENGTH_LONG).show();
                            } catch (Exception ex) {

                            }
                        }
                    })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "Error writing document", e);
                                }
                            });
                } else {
                    Log.e(TAG, "firestore get failed with ", task.getException());
                }
            }
        });
    }

    @Override
    public void onDialogConfirmAdd(Item item, Integer quantity) {
        DocumentReference docRef = cartColRef.document(item.getId().getId());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    CartItem cartItem;
                    if (document.exists()) {
                        cartItem = document.toObject(CartItem.class);
                        cartItem.setQuantity(cartItem.getQuantity() + quantity);
                    } else {
                        cartItem = new CartItem(item);
                        cartItem.setQuantity(quantity);
                    }
                    docRef.set(cartItem).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "Successful write to firestore");
                            try {
                                Toast.makeText(getContext(), "Successfully added to cart", Toast.LENGTH_LONG).show();
                            } catch (Exception ex) {

                            }
                        }
                    })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "Error writing document", e);
                                }
                            });
                } else {
                    Log.e(TAG, "firestore get failed with ", task.getException());
                }
            }
        });
    }

}