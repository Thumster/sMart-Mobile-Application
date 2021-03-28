package com.example.smart.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart.R;
import com.example.smart.model.Transaction;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;

public class TransactionAdapter extends FirestoreAdapter<TransactionAdapter.ViewHolder> {
    private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    private Context context;

    public TransactionAdapter(Query query, Context context) {
        super(query);
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.item_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction tx = getSnapshot(position).toObject(Transaction.class);
        holder.bind(tx, context);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        Boolean extendLayout = false;
        ConstraintLayout transactionLayout;
        ConstraintLayout extendedLayout;
        TextView dateView;
        TextView quantityView;
        TextView priceView;
        RecyclerView transactionItemsRecycler;
        View dividerView;

        public ViewHolder(View itemView) {
            super(itemView);
            transactionLayout = itemView.findViewById(R.id.tx_container);
            extendedLayout = itemView.findViewById(R.id.tx_extended);
            dateView = itemView.findViewById(R.id.tx_date);
            quantityView = itemView.findViewById(R.id.tx_quantity);
            priceView = itemView.findViewById(R.id.tx_price);
            transactionItemsRecycler = itemView.findViewById(R.id.recycler_transaction_items);
            dividerView = itemView.findViewById(R.id.divider);
        }

        public void bind(Transaction tx, Context context) {
            dateView.setText(String.format("%s", sdf.format(tx.getTransactionDateTime())));
            quantityView.setText(String.format("%d Items", tx.getTotalNoOfItems()));
            priceView.setText(String.format("$%.2f", tx.getTotalPrice()));
            TransactionLineItemAdapter lineItemsAdapter = new TransactionLineItemAdapter(tx.getCartItems());
            transactionItemsRecycler.setLayoutManager(new LinearLayoutManager(context));
            transactionItemsRecycler.setAdapter(lineItemsAdapter);

            transactionLayout.setOnClickListener(v -> {
                extendLayout = !extendLayout;
                if (extendLayout) {
                    extendedLayout.setVisibility(View.VISIBLE);
                    dividerView.setVisibility(View.GONE);
                } else {
                    extendedLayout.setVisibility(View.GONE);
                    dividerView.setVisibility(View.VISIBLE);
                }
            });
        }

    }
}
