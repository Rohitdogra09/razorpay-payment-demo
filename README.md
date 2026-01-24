# Razorpay Demo (with Stripe PaymentIntent + Webhook) — Spring Boot + MySQL

A Spring Boot project that demonstrates a complete payment flow using **Stripe PaymentIntents**:
- Create a booking (order)
- Create a Stripe PaymentIntent for that booking
- Pay from the frontend using the `clientSecret`
- Receive Stripe webhook events and update booking status in MySQL

> Status flow in DB: `CREATED` → `PENDING_PAYMENT` → `CONFIRMED` (or `FAILED`)

---

## Tech Stack

- Java 23
- Spring Boot 4.x (Web, JPA)
- MySQL
- Stripe Java SDK
- Stripe CLI (for local webhook forwarding)
- Postman (for API testing)

---

## Features

✅ Create booking stored in MySQL  
✅ Create Stripe PaymentIntent for booking (`/api/payments/create-intent/{bookingId}`)  
✅ Save `paymentIntentId` + `clientSecret` in DB  
✅ Webhook verification using Stripe signature (`Stripe-Signature` header)  
✅ On `payment_intent.succeeded` webhook → update booking status to `CONFIRMED`  
✅ Uses PaymentIntent `metadata.bookingId` to reliably map Stripe payment → booking

---

## Project Structure (high level)

- `Booking` entity contains:
    - `amountRupees`
    - `productName`
    - `stripePaymentIntentId`
    - `stripeClientSecret`
    - `status` (enum)
- `PaymentController` creates PaymentIntent and updates booking to `PENDING_PAYMENT`
- `StripeWebhookController` verifies event signature and confirms booking on successful payment
- `StripeService` initializes Stripe API key and creates PaymentIntent with metadata

---

## Booking Status Enum

```java
public enum Status {
  CREATED,
  PENDING_PAYMENT,
  CONFIRMED,
  FAILED
}
```

## Prerequisites

Java 23 installed
Maven installed
MySQL running locally
Stripe account
Stripe CLI installed
Postman (optional but recommended)

Environment Variables / Config

Configure these in src/main/resources/application.properties:

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/razorpay_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD


spring.jpa.hibernate.ddl-auto=update


# Stripe
stripe.secret.key=sk_test_xxxxxxxxxxxxxxxxx
stripe.webhook.secret=whsec_xxxxxxxxxxxxxxxxx

Run the Backend
mvn spring-boot:run

Server starts on:

http://localhost:8080

Webhook Setup (Local)

Stripe cannot call localhost directly, so use Stripe CLI to forward webhooks:
stripe listen --forward-to http://localhost:8080/api/stripe/webhook

Stripe CLI prints a webhook signing secret like:

Your webhook signing secret is whsec_...

Copy this into:

stripe.webhook.secret=whsec_...

Restart the backend after setting it.

# API Endpoints
1) Create Booking

(Your booking endpoint may differ — update as per your controller)

Example:

POST /api/bookings

Body:
```
{
  "productName": "Apple",
  "amountPaise": 80000
}
```

Response:
```
{
  "id": 11,
  "productName": "Apple",
  "amountPaise": 80000,
  "status": "CREATED"
}
```
# 2) Create Stripe PaymentIntent

POST /api/payments/create-intent/{bookingId}

Example:
POST /api/payments/create-intent/11

Response:

{
  "bookingId": 11,
  "status": "PENDING_PAYMENT",
  "paymentIntentId": "pi_123...",
  "clientSecret": "pi_123_secret_..."
}

This endpoint:

creates Stripe PaymentIntent

sets metadata bookingId

saves paymentIntentId + clientSecret into the booking

updates status to PENDING_PAYMENT

# 3) Stripe Webhook

POST /api/stripe/webhook

This endpoint is called by Stripe (via Stripe CLI forwarding locally).
It:

verifies signature using stripe.webhook.secret

on payment_intent.succeeded:

retrieves PaymentIntent from Stripe

reads metadata.bookingId

updates booking status to CONFIRMED

# 4) Get Booking Status

Example:

GET /api/bookings/{bookingId}

Response after successful payment:
``{
  "id": 11,
  "productName": "Apple",
  "amountPaise": 80000,
  "stripePaymentIntentId": "pi_123...",
  "stripeClientSecret": "pi_123_secret_...",
  "status": "CONFIRMED"
}``


# Frontend Payment Flow (concept)

Frontend uses the clientSecret returned by /create-intent/{bookingId} and confirms payment using Stripe.js (PaymentElement/CardElement).

High-level steps:

Create booking

Create payment intent → get clientSecret

Stripe.js confirms payment with clientSecret

Stripe sends webhook payment_intent.succeeded

Backend updates booking to CONFIRMED

# Testing Checklist

✅ Backend running

✅ Stripe CLI running and forwarding to webhook

✅ stripe.webhook.secret set correctly

✅ Create booking (status CREATED)

✅ Create intent (status PENDING_PAYMENT)

✅ Pay in frontend

✅ Stripe CLI shows:

payment_intent.succeeded → [200]
✅ Booking GET shows:

"status": "CONFIRMED"

# Common Issues
Webhook returns 400 / status not updating

Make sure Stripe CLI is forwarding to the correct endpoint:

http://localhost:8080/api/stripe/webhook

Ensure stripe.webhook.secret matches the one shown by Stripe CLI

Ensure PaymentIntent metadata contains bookingId

Booking stays PENDING_PAYMENT

Check Spring logs for webhook event type and metadata

Confirm webhook received payment_intent.succeeded

Confirm PaymentIntent retrieve returns metadata bookingId

Next Technical Improvements (optional)

Webhook idempotency (avoid double-processing)

Handle payment_intent.payment_failed and payment_intent.canceled

Verify paid amount & currency before confirming booking

Reconciliation endpoint (query Stripe and sync status)
