package com.example.smart.adapter;

import android.content.res.Resources;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.example.smart.R;
import com.example.smart.model.Item;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

public class ItemAdapter extends FirestoreAdapter<ItemAdapter.ViewHolder> {

    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();

    public interface OnItemSelectedListener {

        void onItemSelected(DocumentSnapshot item);
        void onItemAdded(DocumentSnapshot item);

    }

    private OnItemSelectedListener mListener;

    public ItemAdapter(Query query, OnItemSelectedListener listener) {
        super(query);
        mListener = listener;
        viewBinderHelper.setOpenOnlyOne(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        return new ViewHolder(inflater.inflate(R.layout.item_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = getSnapshot(position).toObject(Item.class);
        viewBinderHelper.bind(holder.swipeRevealLayout, item.getId().getId());

        holder.bind(getSnapshot(position), mListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout itemLayout;
        ConstraintLayout addToCartLayout;
        SwipeRevealLayout swipeRevealLayout;
        ImageView imageView;
        TextView nameView;
        TextView categoryView;
        TextView priceView;
        TextView oldPriceView;
        ImageView promotionIconView;
        TextView promotionView;

        public ViewHolder(View itemView) {
            super(itemView);
            itemLayout = itemView.findViewById(R.id.item_layout);
            addToCartLayout = itemView.findViewById(R.id.item_menu);
            swipeRevealLayout = itemView.findViewById(R.id.item_slide_layout);
            imageView = itemView.findViewById(R.id.item_image);
            nameView = itemView.findViewById(R.id.item_name);
            categoryView = itemView.findViewById(R.id.item_category);
            priceView = itemView.findViewById(R.id.item_price);
            oldPriceView = itemView.findViewById(R.id.item_old_price);
            promotionIconView = itemView.findViewById(R.id.item_promotion_icon);
            promotionView = itemView.findViewById(R.id.item_promotion);

        }

        public void bind(final DocumentSnapshot snapshot,
                         final OnItemSelectedListener listener) {

            Item item = snapshot.toObject(Item.class);
            Resources resources = itemView.getResources();

            // Load image
            Glide.with(imageView.getContext())
                    .load(item.getPhoto())
                    .into(imageView);

            nameView.setText(item.getName());
            categoryView.setText(item.getCategory());
            priceView.setText(String.format("$%.2f", item.getPrice()));
            if (item.getOldPrice() != 0.0) {
                oldPriceView.setText(String.format("$%.2f", item.getOldPrice()));
                oldPriceView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                oldPriceView.setVisibility(View.GONE);
                promotionIconView.setVisibility(View.GONE);
                promotionView.setVisibility(View.GONE);
            }

            // Click listener
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemSelected(snapshot);
                    }
                }
            });
            addToCartLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onItemAdded(snapshot);
                        swipeRevealLayout.close(true);
                    }
                }
            });
        }

    }
}
