package com.example.smart.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class CartItem extends Item {

    private Integer quantity;

    private Integer quantityInCart;

    public CartItem() {
    }

    public CartItem(Item item) {
        super(item.getId(), item.getName(), item.getCategory(), item.getPrice(), item.getOldPrice(), item.getPhoto());
        this.quantity = 1;
        this.quantityInCart = 0;
    }

    public CartItem(Item item, Integer quantity, Integer quantityInCart) {
        super(item.getId(), item.getName(), item.getCategory(), item.getPrice(), item.getOldPrice(), item.getPhoto());
        this.quantity = quantity;
        this.quantityInCart = quantityInCart;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantityInCart() {
        return quantityInCart;
    }

    public void setQuantityInCart(Integer quantityInCart) {
        this.quantityInCart = quantityInCart;
    }
}
