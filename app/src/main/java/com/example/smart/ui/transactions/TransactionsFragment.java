package com.example.smart.ui.transactions;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.smart.R;
import com.example.smart.adapter.ItemAdapter;
import com.example.smart.adapter.TransactionAdapter;
import com.example.smart.util.FirebaseUtil;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

public class TransactionsFragment extends Fragment {

    private TransactionAdapter transactionAdapter;

    private RecyclerView transactionsRecycler;
    private ViewGroup emptyView;

    public TransactionsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_transactions, container, false);
        emptyView = root.findViewById(R.id.view_empty);
        transactionsRecycler = root.findViewById(R.id.recycler_transactions);
        initRecyclerView(root);

        return root;
    }

    private void initRecyclerView(View view) {
        Query query = FirebaseUtil.getUserTransactionsRef()
                .orderBy("transactionDateTime", Query.Direction.DESCENDING);
        transactionAdapter = new TransactionAdapter(query, getContext()) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    transactionsRecycler.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    transactionsRecycler.setVisibility(View.VISIBLE);
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

        transactionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsRecycler.setAdapter(transactionAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start listening for Firestore updates
        if (transactionAdapter != null) {
            transactionAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (transactionAdapter != null) {
            transactionAdapter.stopListening();
        }
    }
}