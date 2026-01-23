package com.rohit.razorpay_demo.controller;

import com.rohit.razorpay_demo.entity.Booking;
import com.rohit.razorpay_demo.repository.BookingRepository;
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

    public StripeWebhookController(
            @Value("${stripe.webhook.secret}") String webhookSecret,
            BookingRepository bookingRepository
    ) {
        this.webhookSecret = webhookSecret;
        this.bookingRepository = bookingRepository;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            String payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            if ("payment_intent.succeeded".equals(event.getType())) {

                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);

                if (paymentIntent != null) {
                    Booking booking = bookingRepository
                            .findByStripePaymentIntentId(paymentIntent.getId())
                            .orElse(null);

                    if (booking != null) {
                        booking.setStatus(Booking.Status.CONFIRMED);
                        bookingRepository.save(booking);
                    }
                }
            }

            if ("charge.succeeded".equals(event.getType())) {
                com.stripe.model.Charge charge = (com.stripe.model.Charge) event.getDataObjectDeserializer()
                        .getObject().orElse(null);

                if (charge != null && charge.getPaymentIntent() != null) {
                    Booking booking = bookingRepository
                            .findByStripePaymentIntentId(charge.getPaymentIntent())
                            .orElse(null);

                    if (booking != null) {
                        booking.setStatus(Booking.Status.CONFIRMED);
                        bookingRepository.save(booking);
                    }
                }
            }

            return ResponseEntity.ok("ok");
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Webhook error");
        }
    }

}
