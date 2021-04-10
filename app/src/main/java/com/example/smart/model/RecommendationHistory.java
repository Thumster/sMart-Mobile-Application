package com.example.smart.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

@IgnoreExtraProperties
public class RecommendationHistory implements Serializable {

    @DocumentId
    protected DocumentReference id;
    private String itemId;
    @ServerTimestamp
    private Date recommendationDateTime;

    public RecommendationHistory() {
    }

    public RecommendationHistory(String itemId, Date recommendationDateTime) {
        this.itemId = itemId;
        this.recommendationDateTime = recommendationDateTime;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Date getRecommendationDateTime() {
        return recommendationDateTime;
    }

    public void setRecommendationDateTime(Date recommendationDateTime) {
        this.recommendationDateTime = recommendationDateTime;
    }
}
