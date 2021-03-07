package com.example.smart.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Item POJO.
 */
@IgnoreExtraProperties
public class Item {

    @DocumentId
    protected DocumentReference id;
    protected String name;
    protected String category;
    protected Double price;
    protected Double oldPrice;
    protected String photo;

    public Item() {
    }

    public Item(DocumentReference id, String name, String category, double price, double oldPrice, String photo) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.oldPrice = oldPrice;
        this.photo = photo;
    }

    public DocumentReference getId() {
        return id;
    }

    public void setId(DocumentReference id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(Double oldPrice) {
        this.oldPrice = oldPrice;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
