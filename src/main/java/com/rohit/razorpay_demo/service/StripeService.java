package com.rohit.razorpay_demo.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    private final String secretKey;

    public StripeService(@Value("${stripe.secret.key}") String secretKey){
        this.secretKey=secretKey;
    }

    @PostConstruct
    void init(){
        Stripe.apiKey = secretKey;
    }

    public PaymentIntent createPaymentIntent(long amountMinor, String currency, String bookingId) throws Exception{
        PaymentIntentCreateParams params=
                PaymentIntentCreateParams.builder()
                        .setAmount(amountMinor) //INR or Euro or any other currrency
                        .setCurrency(currency)
                        .putMetadata("bookingId", bookingId)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();

        return PaymentIntent.create(params);
    }
}
