package com.example.smart.ui.cart;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart.R;
import com.example.smart.adapter.CartItemAdapter;
import com.example.smart.model.CartItem;
import com.example.smart.util.FirebaseUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

public class CartFragment extends Fragment implements
        CartItemAdapter.OnItemSelectedListener {

    private static final String TAG = "CART_ITEMS_FRAGMENT";

    private CartItemAdapter cartItemAdaptor;
    private RecyclerView cartItemsRecycler;
    private ViewGroup summaryView;
    private TextView summaryPriceView;
    private TextView summaryItemsNo;
    private ViewGroup emptyView;
    private Button checkoutButton;
    private Button redirectButton;

    private Query query;
    private CollectionReference cartColRef;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cart, container, false);
        cartItemsRecycler = root.findViewById(R.id.recycler_cart);
        summaryView = root.findViewById(R.id.view_summary);
        summaryPriceView = root.findViewById(R.id.summary_price);
        summaryItemsNo = root.findViewById(R.id.summary_items);
        emptyView = root.findViewById(R.id.view_empty);
        checkoutButton = root.findViewById(R.id.button_checkout);
        redirectButton = root.findViewById(R.id.button_redirect_items);

        cartColRef = FirebaseUtil.getUserCartItemsRef();
        query = cartColRef;
        initRecyclerView(root);

        return root;
    }

    private void initRecyclerView(View view) {
        if (query == null) {
            Log.i(TAG, "No query, not initializing cart items RecyclerView");
        }
        cartItemAdaptor = new CartItemAdapter(query, this) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    cartItemsRecycler.setVisibility(View.GONE);
                    summaryView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);

                    redirectButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            NavController navController = Navigation.findNavController(v);
                            Navigation.findNavController(view).navigate(R.id.navigation_items);
                        }
                    });
                } else {
                    cartItemsRecycler.setVisibility(View.VISIBLE);
                    summaryView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);

                    summaryPriceView.setText(String.format("$%.2f", cartItemAdaptor.getTotalPriceOfCart()));
                    Integer noOfItems = cartItemAdaptor.getTotalItemsInCart();
                    String noOfItemsText = "";
                    if (noOfItems > 1) {
                        summaryItemsNo.setText(String.format("%d Items", noOfItems));
                    } else {
                        summaryItemsNo.setText(String.format("%d Item", noOfItems));
                    }
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

        if (itemSelected.getQuantityInCart() > 0) {
            itemSelected.setQuantity(itemSelected.getQuantityInCart());
            docRef.set(itemSelected).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(TAG, "Successful write to firestore");
                    Toast.makeText(getContext(), "Item detected in cart, only deleted excess", Toast.LENGTH_LONG).show();
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Error writing document", e);
                        }
                    });
        } else {
            docRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(TAG, "Successful delete from firestore");
                    Toast.makeText(getContext(), "Successfully deleted from cart", Toast.LENGTH_LONG).show();
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Error deleting document", e);
                        }
                    });
        }
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
                Log.i(TAG, "Successful update to firestore");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error updating document", e);
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
                Log.i(TAG, "Successful update to firestore");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "Error updating document", e);
                    }
                });
    }
}