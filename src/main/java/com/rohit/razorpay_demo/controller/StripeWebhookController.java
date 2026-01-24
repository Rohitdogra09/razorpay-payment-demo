package com.rohit.razorpay_demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohit.razorpay_demo.entity.Booking;
import com.rohit.razorpay_demo.repository.BookingRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private final String webhookSecret;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StripeWebhookController(
            @Value("${stripe.webhook.secret}") String webhookSecret,
            @Value("${stripe.secret.key}") String stripeSecretKey,
            BookingRepository bookingRepository
    ) {
        this.webhookSecret = webhookSecret;
        this.bookingRepository = bookingRepository;

        // Needed because we call PaymentIntent.retrieve() inside webhook
        Stripe.apiKey = stripeSecretKey;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            String payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            // 1) Verify signature + parse event
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            System.out.println("✅ Webhook received: " + event.getType() + " id=" + event.getId());

            // 2) Handle success
            if ("payment_intent.succeeded".equals(event.getType())) {

                // Extract PI id from raw event JSON (always present)
                JsonNode root = objectMapper.readTree(payload);
                String paymentIntentId = root.path("data").path("object").path("id").asText(null);

                if (paymentIntentId == null || paymentIntentId.isBlank()) {
                    System.out.println("⚠️ payment_intent id missing in event " + event.getId());
                    return ResponseEntity.badRequest().body("Missing payment_intent id");
                }

                // Retrieve full PI from Stripe to reliably access metadata
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

                System.out.println("✅ PI: " + paymentIntent.getId());
                System.out.println("✅ Metadata: " + paymentIntent.getMetadata());

                String bookingIdStr = paymentIntent.getMetadata().get("bookingId");
                if (bookingIdStr == null || bookingIdStr.isBlank()) {
                    System.out.println("⚠️ bookingId metadata missing for PI " + paymentIntent.getId());
                    return ResponseEntity.ok("ok"); // don't fail webhook; just log
                }

                Long bookingId = Long.valueOf(bookingIdStr);

                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

                booking.setStatus(Booking.Status.CONFIRMED);
                bookingRepository.save(booking);

                System.out.println("✅ Booking " + booking.getId() + " marked CONFIRMED");
            }

            // (Optional) Handle payment failure too
            if ("payment_intent.payment_failed".equals(event.getType())) {

                JsonNode root = objectMapper.readTree(payload);
                String paymentIntentId = root.path("data").path("object").path("id").asText(null);

                if (paymentIntentId != null && !paymentIntentId.isBlank()) {
                    PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                    String bookingIdStr = paymentIntent.getMetadata().get("bookingId");

                    if (bookingIdStr != null && !bookingIdStr.isBlank()) {
                        Long bookingId = Long.valueOf(bookingIdStr);
                        bookingRepository.findById(bookingId).ifPresent(b -> {
                            b.setStatus(Booking.Status.FAILED);
                            bookingRepository.save(b);
                            System.out.println("✅ Booking " + b.getId() + " marked FAILED");
                        });
                    }
                }
            }

            return ResponseEntity.ok("ok");

        } catch (SignatureVerificationException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Invalid signature: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }
}