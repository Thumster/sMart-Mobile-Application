package com.example.smart.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart.R;
import com.example.smart.model.CartItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ReceiptItemAdapter extends FirestoreAdapter<ReceiptItemAdapter.ViewHolder> {

    public ReceiptItemAdapter(Query query) {
        super(query);
    }

    public Double getTotalPriceOfReceipt() {
        if (super.getItemCount() > 0) {
            ArrayList<DocumentSnapshot> list = super.mSnapshots;
            Double sum = 0.0;
            for (DocumentSnapshot snapshot : list) {
                CartItem cartItem = snapshot.toObject(CartItem.class);
                sum += cartItem.getPrice() * cartItem.getQuantityInCart();
            }
            return sum;
        }
        return 0.0;
    }

    public Integer getTotalItemsInReceipt() {
        if (super.getItemCount() > 0) {
            ArrayList<DocumentSnapshot> list = super.mSnapshots;
            Integer sum = 0;
            for (DocumentSnapshot snapshot : list) {
                CartItem cartItem = snapshot.toObject(CartItem.class);
                sum += cartItem.getQuantityInCart();
            }
            return sum;
        }
        return 0;
    }

    public List<CartItem> getListOfCartItems() {
        ArrayList<CartItem> cartItems = new ArrayList<>();
        for (DocumentSnapshot snapshot: super.mSnapshots) {
            CartItem cartItem = snapshot.toObject(CartItem.class);
            cartItems.add(cartItem);
        }
        return cartItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.item_receipt, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptItemAdapter.ViewHolder holder, int position) {
        CartItem item = getSnapshot(position).toObject(CartItem.class);
        holder.bind(item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        TextView priceView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_name);
            priceView = itemView.findViewById(R.id.item_price);
        }

        public void bind(CartItem item) {
            nameView.setText(String.format("%2d X %s",item.getQuantityInCart(), item.getName()));
            priceView.setText(String.format("$%.2f", item.getPrice() * item.getQuantityInCart()));
        }

    }
}
