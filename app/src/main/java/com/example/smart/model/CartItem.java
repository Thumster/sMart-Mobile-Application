package com.example.smart.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class CartItem extends Item{

    private Integer quantity;

    private Integer quantityInCart;

    public CartItem() {
    }

    public CartItem(Item item) {
        super(item.getId(),item.getName(), item.getCategory(), item.getPrice(), item.getOldPrice(), item.getPhoto());
        this.quantity = 1;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
