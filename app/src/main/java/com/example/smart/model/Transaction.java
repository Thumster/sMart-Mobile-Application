package com.example.smart.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Item POJO.
 */
public class Transaction implements Serializable {

    @DocumentId
    private DocumentReference id;
    @ServerTimestamp
    private Date transactionDateTime;
    private Integer totalNoOfItems;
    private Double totalPrice;
    private List<CartItem> cartItems;

    public Transaction() {
        totalNoOfItems = 0;
        totalPrice = 0.0;
        cartItems = new ArrayList<>();
    }

    public Transaction(List<CartItem> cartItems) {
        this();
        for (CartItem cartItem : cartItems) {
            this.totalNoOfItems += cartItem.getQuantityInCart();
            this.totalPrice += cartItem.getPrice() * cartItem.getQuantityInCart();
        }
        this.cartItems = cartItems;
        this.transactionDateTime = new Date();
    }

    public DocumentReference getId() {
        return id;
    }

    public void setId(DocumentReference id) {
        this.id = id;
    }

    public Date getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(Date transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public Integer getTotalNoOfItems() {
        return totalNoOfItems;
    }

    public void setTotalNoOfItems(Integer totalNoOfItems) {
        this.totalNoOfItems = totalNoOfItems;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }
}
