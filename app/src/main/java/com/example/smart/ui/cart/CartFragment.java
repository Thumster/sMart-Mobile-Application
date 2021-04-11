package com.example.smart.ui.cart;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment implements
        CartItemAdapter.OnItemSelectedListener, FirebaseUtil.OnCartFound {

    private static final String TAG = "CART_ITEMS_FRAGMENT";

    private CartItemAdapter cartItemAdaptor;
    private RecyclerView cartItemsRecycler;
    private ViewGroup summaryView;
    private TextView summaryPriceView;
    private TextView summaryItemsNo;
    private ViewGroup emptyView;
    private Button checkoutButton;
    private Button redirectButton;

    private TextView inShopSummaryPriceView;
    private TextView inShopSummaryItemsNo;
    private ImageView inShopIcon;

    private ViewGroup cartInfoView;
    private TextView userCartTextView;
    private ImageView buttonUnregisterCart;

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

        inShopSummaryPriceView = root.findViewById(R.id.in_shop_summary_price);
        inShopSummaryItemsNo = root.findViewById(R.id.in_shop_summary_items);
        inShopIcon = root.findViewById(R.id.in_shop_icon_cart);

        cartInfoView = root.findViewById(R.id.view_cart_info);
        buttonUnregisterCart = root.findViewById(R.id.button_unregister_cart);
        userCartTextView = root.findViewById(R.id.text_user_cart);

        cartColRef = FirebaseUtil.getUserCartItemsRef();
        query = cartColRef;
        initRecyclerView(root);

        FirebaseUtil.startListening(this);
        onCartFound(FirebaseUtil.getIsCurrentlyShopping());
        buttonUnregisterCart.setOnClickListener(v -> {
            unregisterCart();
        });

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

                    String physicalCartTotalPrice = String.format("$%.2f", cartItemAdaptor.getTotalPriceOfPhysicalCart());
                    Integer physicalCartTotalItems = cartItemAdaptor.getTotalItemsInPhysicalCart();
                    if (physicalCartTotalItems > 0) {
                        buttonUnregisterCart.setVisibility(View.GONE);
                    } else {
                        buttonUnregisterCart.setVisibility(View.VISIBLE);
                    }
                    if (FirebaseUtil.getIsCurrentlyShopping()) {
                        checkoutButton.setVisibility(View.VISIBLE);
                        inShopSummaryItemsNo.setVisibility(View.VISIBLE);
                        inShopSummaryPriceView.setVisibility(View.VISIBLE);
                        inShopIcon.setVisibility(View.VISIBLE);

                        inShopSummaryPriceView.setText(physicalCartTotalPrice);
                        inShopSummaryItemsNo.setText(String.format("%d", physicalCartTotalItems));
                    } else {
                        checkoutButton.setVisibility(View.GONE);
                        inShopSummaryItemsNo.setVisibility(View.GONE);
                        inShopSummaryPriceView.setVisibility(View.GONE);
                        inShopIcon.setVisibility(View.GONE);
                    }

                    summaryPriceView.setText(String.format("$%.2f", cartItemAdaptor.getTotalPriceOfCart()));
                    Integer noOfItems = cartItemAdaptor.getTotalItemsInCart();
                    if (noOfItems > 1) {
                        summaryItemsNo.setText(String.format("%d Items", noOfItems));
                    } else {
                        summaryItemsNo.setText(String.format("%d Item", noOfItems));
                    }

                    Integer noOfItemsInPhysicalCart = cartItemAdaptor.getTotalItemsInPhysicalCart();

                    checkoutButton.setOnClickListener(v -> {
                        if (noOfItemsInPhysicalCart > 0) {
                            Query query = cartColRef.whereGreaterThan("quantityInCart", 0);
                            CheckoutDialogFragment checkoutDialog = new CheckoutDialogFragment(new CheckoutDialogFragment.CheckoutDialogListener() {
                                @Override
                                public void onCheckout(List<CartItem> purchasedCartItems) {
                                    cartColRef.get().addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            FirebaseUtil.setCurrentUserCartId(null);
                                            FirebaseUtil.setIsCurrentlyShopping(false);
                                            onCartFound(FirebaseUtil.getIsCurrentlyShopping());
                                            List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                                            for (DocumentSnapshot snapshot : snapshots) {
                                                CartItem cartItem = snapshot.toObject(CartItem.class);
                                                purchasedCartItems.stream()
                                                        .filter(item -> item.getId().equals(cartItem.getId()))
                                                        .findFirst()
                                                        .ifPresent(foundItem -> {
                                                            Integer newQuantity = cartItem.getQuantity() - foundItem.getQuantityInCart();
                                                            if (newQuantity > 0) {
                                                                cartItem.setQuantity(newQuantity);
                                                                cartItem.setQuantityInCart(0);
                                                                snapshot.getReference().set(cartItem);
                                                            } else {
                                                                snapshot.getReference().delete();
                                                            }
                                                        });
                                            }
                                        }
                                    });
                                }
                            }, query);
                            checkoutDialog.show(getFragmentManager(), TAG);
                        } else {
                            Toast.makeText(getContext(), "No items detected in physical cart!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
                // Show a snackbar on errors
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

    private void unregisterCart() {
        Log.i(TAG, "ATTEMPTING TO UNREGISTER CART ID: " + FirebaseUtil.getCurrentUserCartId());
        if (!FirebaseUtil.getIsCurrentlyShopping()) {
            Log.i(TAG, "User is not registered to a shopping cart");
            Toast.makeText(getContext(), "You are not currently shopping", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        DocumentReference cartDocRef = FirebaseUtil.getCartsRef()
                .document(FirebaseUtil.getCurrentUserCartId());
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirebaseUtil.CART_USER_DOC_NAME, FieldValue.delete());
        cartDocRef.update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "Successfully unregistered cart");
                        onCartFound(false);
                        FirebaseUtil.setCurrentUserCartId(null);
                        FirebaseUtil.setIsCurrentlyShopping(false);
                        Toast.makeText(getContext(), "Successfully unregistered cart!", Toast.LENGTH_LONG)
                                .show();
                    } else {
                        Log.e(TAG, "firestore update failed with ", task.getException());
                    }
                });
    }

    @Override
    public void onCartFound(Boolean found) {
        if (found) {
            userCartTextView.setText(FirebaseUtil.getCurrentUserCartId());
            cartInfoView.setVisibility(View.VISIBLE);
            FirebaseUtil.setIsCurrentlyShopping(true);
        } else {
            cartInfoView.setVisibility(View.GONE);
            FirebaseUtil.setIsCurrentlyShopping(false);
        }
    }

    @Override
    public void onItemDeleted(DocumentSnapshot item) {
        CartItem itemSelected = item.toObject(CartItem.class);
        String itemId = itemSelected.getId().getId();
        DocumentReference docRef = cartColRef.document(itemId);

        if (itemSelected.getQuantityInCart() > 0) {
            Toast.makeText(getContext(), "Item in cart. Unable to delete!", Toast.LENGTH_LONG).show();
            return;
        }
        if (itemSelected.getQuantityInCart() > 0) {
            itemSelected.setQuantity(itemSelected.getQuantityInCart());
            docRef.set(itemSelected).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(TAG, "Successful write to firestore");
                    try {
                        Toast.makeText(getContext(), "Item detected in cart, only deleted excess", Toast.LENGTH_LONG).show();
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
            docRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i(TAG, "Successful delete from firestore");
                    try {
                        Toast.makeText(getContext(), "Successfully deleted from cart", Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {

                    }
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Error deleting document", e);
                        }
                    });
        }
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