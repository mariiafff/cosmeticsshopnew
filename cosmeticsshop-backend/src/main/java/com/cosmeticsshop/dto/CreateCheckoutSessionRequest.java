package com.cosmeticsshop.dto;

import java.util.ArrayList;
import java.util.List;

public class CreateCheckoutSessionRequest {

    private List<CheckoutLineItemRequest> items = new ArrayList<>();
    private String successUrl;
    private String cancelUrl;

    public List<CheckoutLineItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CheckoutLineItemRequest> items) {
        this.items = items;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }
}
