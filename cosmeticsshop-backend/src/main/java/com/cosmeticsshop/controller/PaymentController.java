package com.cosmeticsshop.controller;

import com.cosmeticsshop.dto.CheckoutLineItemRequest;
import com.cosmeticsshop.dto.CheckoutSessionResponse;
import com.cosmeticsshop.dto.CreateCheckoutSessionRequest;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final String stripeSecretKey;
    private final String frontendUrl;

    public PaymentController(
            @Value("${stripe.secret.key}") String stripeSecretKey,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.stripeSecretKey = stripeSecretKey;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @RequestBody CreateCheckoutSessionRequest request
    ) throws StripeException {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }

        Stripe.apiKey = stripeSecretKey;
        String successUrl = request.getSuccessUrl() == null || request.getSuccessUrl().isBlank()
                ? frontendUrl + "/orders/payment-success?payment=success"
                : request.getSuccessUrl();
        String cancelUrl = request.getCancelUrl() == null || request.getCancelUrl().isBlank()
                ? frontendUrl + "/checkout?payment=cancelled"
                : request.getCancelUrl();

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl);

        for (CheckoutLineItemRequest item : request.getItems()) {
            long unitAmount = Math.max(50L, Math.round((item.getPrice() == null ? 0 : item.getPrice()) * 100));
            long quantity = Math.max(1L, item.getQuantity() == null ? 1L : item.getQuantity().longValue());
            String name = item.getName() == null || item.getName().isBlank() ? "Luime product" : item.getName();

            params.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(quantity)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(unitAmount)
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(name)
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        Session session = Session.create(params.build());
        return ResponseEntity.ok(new CheckoutSessionResponse(session.getUrl()));
    }
}
