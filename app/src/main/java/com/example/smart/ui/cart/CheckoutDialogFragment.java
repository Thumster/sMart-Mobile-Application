package com.example.smart.ui.cart;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart.R;
import com.example.smart.adapter.ReceiptItemAdapter;
import com.example.smart.model.CartItem;
import com.example.smart.model.Transaction;
import com.example.smart.util.FirebaseUtil;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutDialogFragment extends DialogFragment {

    private Query query;
    private ReceiptItemAdapter receiptItemAdapter;
    private CheckoutDialogListener checkoutDialogListener;
    private RecyclerView checkoutRecycler;
    private ImageView closeView;
    private TextView itemsView;
    private TextView itemsPriceView;
    private Button confirmButton;
    public CheckoutDialogFragment(CheckoutDialogListener listener, Query query) {
        this.checkoutDialogListener = listener;
        this.query = query;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_checkout, null);

        checkoutRecycler = view.findViewById(R.id.recycler_checkout_items);
        closeView = view.findViewById(R.id.checkout_close);
        itemsView = view.findViewById(R.id.checkout_items);
        itemsPriceView = view.findViewById(R.id.checkout_price);
        confirmButton = view.findViewById(R.id.button_confirm);

        closeView.setOnClickListener(v -> {
            dismiss();
        });

        confirmButton.setOnClickListener(v -> {
            if (receiptItemAdapter.getTotalItemsInReceipt() > 0) {
                CollectionReference colRef = FirebaseUtil.getUserTransactionsRef();
                List<CartItem> cartItems = receiptItemAdapter.getListOfCartItems();
                Transaction transaction = new Transaction(cartItems);
                colRef.add(transaction);

                // STOP SHOPPING HERE
                DocumentReference cartDocRef = FirebaseUtil.getCartsRef().document(FirebaseUtil.getCurrentUserCartId());
                Map<String, Object> updates = new HashMap<>();
                updates.put(FirebaseUtil.CART_USER_DOC_NAME, FieldValue.delete());
                cartDocRef.update(updates);

                checkoutDialogListener.onCheckout(cartItems);


                Toast.makeText(getContext(), "Successful Checkout", Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(getContext(), "No items detected in physical cart!", Toast.LENGTH_LONG).show();
            }
            dismiss();
        });

        initRecyclerView(view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }

    private void initRecyclerView(View view) {
        receiptItemAdapter = new ReceiptItemAdapter(query) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    checkoutRecycler.setVisibility(View.GONE);
                } else {
                    checkoutRecycler.setVisibility(View.VISIBLE);

                    Integer physicalCartTotalItems = receiptItemAdapter.getTotalItemsInReceipt();
                    String physicalCartTotalPrice = String.format("$%.2f", receiptItemAdapter.getTotalPriceOfReceipt());
                    if (physicalCartTotalItems > 1) {
                        itemsView.setText(String.format("%d Items", physicalCartTotalItems));
                    } else {
                        itemsView.setText(String.format("%d Item", physicalCartTotalItems));
                    }
                    itemsPriceView.setText(physicalCartTotalPrice);
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
                // Show a snackbar on errors
                Snackbar.make(view.findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }

        };

        checkoutRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        checkoutRecycler.setAdapter(receiptItemAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (receiptItemAdapter != null) {
            receiptItemAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (receiptItemAdapter != null) {
            receiptItemAdapter.stopListening();
        }
    }

    public interface CheckoutDialogListener {
        void onCheckout(List<CartItem> cartItems);
    }

}
