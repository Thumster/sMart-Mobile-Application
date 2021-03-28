package com.example.smart.adapter;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.example.smart.R;
import com.example.smart.model.CartItem;
import com.example.smart.model.Item;
import com.example.smart.util.FirebaseUtil;
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

    public Integer getTotalItemsInCart() {
        if (super.getItemCount() > 0) {
            ArrayList<DocumentSnapshot> list = super.mSnapshots;
            Integer sum = 0;
            for (DocumentSnapshot snapshot: list) {
                CartItem cartItem = snapshot.toObject(CartItem.class);
                sum += cartItem.getQuantity();
            }
            return sum;
        }
        return 0;
    }

    public Double getTotalPriceOfPhysicalCart() {
        if (super.getItemCount() > 0) {
            ArrayList<DocumentSnapshot> list = super.mSnapshots;
            Double sum = 0.0;
            for (DocumentSnapshot snapshot: list) {
                CartItem cartItem = snapshot.toObject(CartItem.class);
                sum += cartItem.getPrice() * cartItem.getQuantityInCart();
            }
            return sum;
        }
        return 0.0;
    }

    public Integer getTotalItemsInPhysicalCart() {
        if (super.getItemCount() > 0) {
            ArrayList<DocumentSnapshot> list = super.mSnapshots;
            Integer sum = 0;
            for (DocumentSnapshot snapshot: list) {
                CartItem cartItem = snapshot.toObject(CartItem.class);
                sum += cartItem.getQuantityInCart();
            }
            return sum;
        }
        return 0;
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

        View inShopLayout;
        ImageView inShopIcon;
        TextView inShopQuantityView;
        TextView inShopPriceView;

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

            inShopLayout = itemView.findViewById(R.id.in_shop_layout);
            inShopIcon = itemView.findViewById(R.id.in_shop_icon_cart);
            inShopQuantityView = itemView.findViewById(R.id.in_shop_quantity);
            inShopPriceView = itemView.findViewById(R.id.in_shop_price);
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

            if (FirebaseUtil.getIsCurrentlyShopping()) {
                inShopLayout.setVisibility(View.VISIBLE);
                inShopQuantityView.setText(item.getQuantityInCart().toString());
                inShopPriceView.setText(String.format("$%.2f",item.getQuantityInCart() * item.getPrice()));
                totalPriceView.setTextAppearance(R.style.Theme_SMart_Subheader);
                totalPriceView.setTypeface(null, Typeface.BOLD);

                if (item.getQuantityInCart() < item.getQuantity()) {
                    inShopIcon.setImageResource(R.drawable.ic_smart_cart_orange_transparent);
                    inShopQuantityView.setTextColor(Color.parseColor("#FFFF8800"));
                } else {
                    inShopIcon.setImageResource(R.drawable.ic_smart_cart_green_transparent);
                    inShopQuantityView.setTextColor(Color.parseColor("#21690F"));
                }

            } else {
                inShopLayout.setVisibility(View.GONE);
                totalPriceView.setTextAppearance(R.style.Theme_SMart_Title);
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
