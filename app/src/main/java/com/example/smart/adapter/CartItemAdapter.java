package com.example.smart.adapter;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.example.smart.R;
import com.example.smart.model.CartItem;
import com.example.smart.model.Item;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

public class CartItemAdapter extends FirestoreAdapter<CartItemAdapter.ViewHolder> {

    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();

    public interface OnItemSelectedListener {

        void onItemDeleted(DocumentSnapshot item);

        void onItemAdd(DocumentSnapshot item);
        void onItemMinus(DocumentSnapshot item);

    }

    private OnItemSelectedListener mListener;

    public CartItemAdapter(Query query, OnItemSelectedListener listener) {
        super(query);
        mListener = listener;
        viewBinderHelper.setOpenOnlyOne(true);
    }

    public Double getTotalPriceOfCart() {
        if (super.getItemCount() > 0) {
            ArrayList<DocumentSnapshot> list = super.mSnapshots;
            Double sum = 0.0;
            for (DocumentSnapshot snapshot: list) {
                CartItem cartItem = snapshot.toObject(CartItem.class);
                sum += cartItem.getPrice() * cartItem.getQuantity();
            }
            return sum;
        }
        return 0.0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.item_cart, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = getSnapshot(position).toObject(Item.class);
        viewBinderHelper.bind(holder.swipeRevealLayout, item.getId().getId());

        holder.bind(getSnapshot(position), mListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout deleteLayout;
        SwipeRevealLayout swipeRevealLayout;
        ImageView imageView;
        TextView nameView;
        TextView categoryView;
        TextView priceView;

        TextView totalPriceView;
        TextView quantityView;
        Button addButton;
        Button minusButton;

        public ViewHolder(View itemView) {
            super(itemView);
            deleteLayout = itemView.findViewById(R.id.item_menu);
            swipeRevealLayout = itemView.findViewById(R.id.item_slide_layout);
            imageView = itemView.findViewById(R.id.item_image);
            nameView = itemView.findViewById(R.id.item_name);
            categoryView = itemView.findViewById(R.id.item_category);
            priceView = itemView.findViewById(R.id.item_price);

            totalPriceView = itemView.findViewById(R.id.item_total_price);
            quantityView = itemView.findViewById(R.id.item_quantity);
            addButton = itemView.findViewById(R.id.item_button_plus);
            minusButton = itemView.findViewById(R.id.item_button_minus);

        }

        public void bind(final DocumentSnapshot snapshot,
                         final OnItemSelectedListener listener) {

            CartItem item = snapshot.toObject(CartItem.class);
            Resources resources = itemView.getResources();

            // Load image
            Glide.with(imageView.getContext())
                    .load(item.getPhoto())
                    .into(imageView);

            nameView.setText(item.getName());
            categoryView.setText(item.getCategory());
            priceView.setText(String.format("$%.2f", item.getPrice()));
            quantityView.setText(item.getQuantity().toString());

            totalPriceView.setText(String.format("$%.2f",item.getQuantity() * item.getPrice()));
            if (item.getQuantity() == 1) {
                minusButton.setEnabled(false);
            } else {
                minusButton.setEnabled(true);
            }

            // Click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    if (listener != null) {
//                        listener.onItemSelected(snapshot);
//                    }
                }
            });
            deleteLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemDeleted(snapshot);
                    }
                }
            });
            addButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemAdd(snapshot);
                    }
                }
            });
            minusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemMinus(snapshot);
                    }
                }
            });
        }

    }
}
