package com.rohit.razorpay_demo.entity;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name ="booking")
public class Booking {

    public enum Status{
        CREATED,
        PENDING_PAYMENT,
        CONFIRMED,
        FAILED
    }

    private String stripePaymentIntentId;
    private String stripeClientSecret;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    //store amount in smallest unit(paise -> indian smallest currency) for Razorpay
    private Long amountPaise;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String razorpayOrderId;
    private String razorpayPaymentId;

    private Instant createdAt;

    @PrePersist
    void onCreate(){
        this.createdAt=Instant.now();
        if(this.status==null) this.status=Status.CREATED;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripeClientSecret() {
        return stripeClientSecret;
    }

    public void setStripeClientSecret(String stripeClientSecret) {
        this.stripeClientSecret = stripeClientSecret;
    }

    // ----- Getters/Setters -----
    public Long getId() { return id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public Instant getCreatedAt() { return createdAt; }
}


