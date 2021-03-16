package com.example.smart.ui.item;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.smart.R;
import com.example.smart.model.Item;

public class ItemDialogFragment extends DialogFragment {
    public interface ItemDialogListener {
        public void onDialogConfirmAdd(Item item, Integer quantity);
    }
    private Item item;
    private Integer quantity;
    private ItemDialogListener itemDialogListener;

    public ItemDialogFragment(ItemDialogFragment.ItemDialogListener listener, Item item) {
        itemDialogListener = listener;
        this.quantity = 1;
        this.item = item;
    }

    ImageView closeView;
    ImageView imageView;
    TextView nameText;
    TextView categoryText;
    ImageView promotionIconView;
    TextView promotionText;
    TextView priceText;
    TextView oldPriceText;
    Button minusButton;
    Button plusButton;
    TextView quantityText;
    TextView totalPriceText;

    Button addToCartButton;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_item_item, null);

        closeView = view.findViewById(R.id.item_close);
        imageView = view.findViewById(R.id.item_image);
        nameText = view.findViewById(R.id.item_name);
        categoryText = view.findViewById(R.id.item_category);
        promotionIconView = view.findViewById(R.id.item_promotion_icon);
        promotionText = view.findViewById(R.id.item_promotion);
        priceText = view.findViewById(R.id.item_price);
        oldPriceText = view.findViewById(R.id.item_old_price);
        minusButton = view.findViewById(R.id.item_button_minus);
        quantityText = view.findViewById(R.id.item_quantity);
        plusButton = view.findViewById(R.id.item_button_plus);
        totalPriceText = view.findViewById(R.id.item_total_price);
        addToCartButton = view.findViewById(R.id.button_add_to_cart);

        closeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        Glide.with(imageView.getContext())
                .load(item.getPhoto())
                .into(imageView);

        nameText.setText(item.getName());
        categoryText.setText(item.getCategory());

        priceText.setText(String.format("$%.2f", item.getPrice()));
        if (item.getOldPrice() != 0.0) {
            oldPriceText.setText(String.format("$%.2f", item.getOldPrice()));
            oldPriceText.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            oldPriceText.setVisibility(View.GONE);
            promotionIconView.setVisibility(View.GONE);
            promotionText.setVisibility(View.GONE);
        }


        quantityText.setText(quantity.toString());
        minusButton.setEnabled(false);
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quantity--;
                if (quantity == 1) {
                    minusButton.setEnabled(false);
                } else {
                    minusButton.setEnabled(true);
                }
                quantityText.setText(quantity.toString());
                totalPriceText.setText(String.format("$%.2f", item.getPrice()*quantity));
            }
        });
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quantity++;
                if (quantity == 1) {
                    minusButton.setEnabled(false);
                } else {
                    minusButton.setEnabled(true);
                }
                quantityText.setText(quantity.toString());
                totalPriceText.setText(String.format("$%.2f", item.getPrice()*quantity));
            }
        });
        totalPriceText.setText(String.format("$%.2f", item.getPrice()*quantity));

        addToCartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemDialogListener != null) {
                    itemDialogListener.onDialogConfirmAdd(item, quantity);
                    dismiss();
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }
}
