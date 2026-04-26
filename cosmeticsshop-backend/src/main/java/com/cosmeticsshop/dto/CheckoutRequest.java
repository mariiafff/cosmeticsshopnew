package com.cosmeticsshop.dto;

import java.util.ArrayList;
import java.util.List;

public class CheckoutRequest {

    private List<CheckoutItemRequest> items = new ArrayList<>();
    private String paymentMethod;

    public List<CheckoutItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CheckoutItemRequest> items) {
        this.items = items;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
