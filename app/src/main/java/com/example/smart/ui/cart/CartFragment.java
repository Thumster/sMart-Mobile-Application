package com.example.smart.ui.cart;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart.R;
import com.example.smart.adapter.CartItemAdapter;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

public class CartFragment extends Fragment implements
        CartItemAdapter.OnItemSelectedListener {

    private static final String TAG = "CART_ITEMS_FRAGMENT";

    private CartItemAdapter cartItemAdaptor;
    private RecyclerView cartItemsRecycler;
    private ViewGroup summaryView;
    private TextView summaryPriceView;
    private ViewGroup emptyView;

    private final String CART_COLLECTION_NAME = "cart";

    private FirebaseFirestore firestore;
    private Query query;
    private CollectionReference cartColRef;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cart, container, false);
        cartItemsRecycler = root.findViewById(R.id.recycler_cart);
        summaryView = root.findViewById(R.id.view_summary);
        summaryPriceView = root.findViewById(R.id.summary_price);
        emptyView = root.findViewById(R.id.view_empty);

        firestore = FirebaseUtil.getFirestore();
        cartColRef = firestore.collection(CART_COLLECTION_NAME);
        query = cartColRef;
        initRecyclerView(root);

        return root;
    }

    private void initRecyclerView(View view) {
        if (query == null) {
            Log.w(TAG, "No query, not initializing cart items RecyclerView");
        }
        cartItemAdaptor = new CartItemAdapter(query, this) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    cartItemsRecycler.setVisibility(View.GONE);
                    summaryView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    cartItemsRecycler.setVisibility(View.VISIBLE);
                    summaryView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    summaryPriceView.setText(String.format("$%.2f", cartItemAdaptor.getTotalPriceOfCart()));
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
//                // Show a snackbar on errors
                Snackbar.make(view.findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }

        };

        cartItemsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        cartItemsRecycler.setAdapter(cartItemAdaptor);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start listening for Firestore updates
        if (cartItemAdaptor != null) {
            cartItemAdaptor.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (cartItemAdaptor != null) {
            cartItemAdaptor.stopListening();
        }
    }

    @Override
    public void onItemDeleted(DocumentSnapshot item) {
        CartItem itemSelected = item.toObject(CartItem.class);
        String itemId = itemSelected.getId().getId();
        DocumentReference docRef = cartColRef.document(itemId);
        docRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.w(TAG, "Successful delete from firestore");
                Toast.makeText(getContext(), "Successfully deleted from cart", Toast.LENGTH_LONG).show();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting document", e);
                    }
                });
        // Go to the details page for the selected restaurant
//        Intent intent = new Intent(this, ItemDetailActivity.class);
//        intent.putExtra(RestaurantDetailActivity.KEY_RESTAURANT_ID, item.getId());
//
//        startActivity(intent);
    }

    @Override
    public void onItemAdd(DocumentSnapshot item) {
        CartItem itemSelected = item.toObject(CartItem.class);
        String itemId = itemSelected.getId().getId();

        itemSelected.setQuantity(itemSelected.getQuantity() + 1);
        DocumentReference docRef = cartColRef.document(itemId);
        docRef.set(itemSelected).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.w(TAG, "Successful update to firestore");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });
    }

    @Override
    public void onItemMinus(DocumentSnapshot item) {
        CartItem itemSelected = item.toObject(CartItem.class);
        String itemId = itemSelected.getId().getId();

        itemSelected.setQuantity(itemSelected.getQuantity() - 1);
        DocumentReference docRef = cartColRef.document(itemId);
        docRef.set(itemSelected).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.w(TAG, "Successful update to firestore");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });
    }
}