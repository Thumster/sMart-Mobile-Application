package com.example.smart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart.R;
import com.example.smart.model.CartItem;

import java.util.List;

public class TransactionLineItemAdapter extends
        RecyclerView.Adapter<TransactionLineItemAdapter.ViewHolder> {

    private List<CartItem> transactionLineItems;

    public TransactionLineItemAdapter(List<CartItem> transactionLineItems) {
        this.transactionLineItems = transactionLineItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.item_receipt, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionLineItemAdapter.ViewHolder holder, int position) {
        CartItem item = transactionLineItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return transactionLineItems.size();
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
            nameView.setText(String.format("%2d X %s", item.getQuantityInCart(), item.getName()));
            priceView.setText(String.format("$%.2f", item.getPrice() * item.getQuantityInCart()));
        }

    }
}
