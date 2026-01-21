package com.rohit.razorpay_demo.controller;

import com.rohit.razorpay_demo.entity.Booking;
import com.rohit.razorpay_demo.repository.BookingRepository;
import com.rohit.razorpay_demo.service.StripeService;
import com.stripe.model.PaymentIntent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.print.Book;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final BookingRepository bookingRepository;
    private final StripeService stripeService;

    public PaymentController(BookingRepository bookingRepository, StripeService stripeService){
        this.bookingRepository=bookingRepository;
        this.stripeService=stripeService;
    }

    @PostMapping("/create-intent/{bookingId}")

    public ResponseEntity<?> createIntent(@PathVariable Long bookingId){
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if(booking == null){
            return ResponseEntity.notFound().build();
        }
        try {
            //use INR for demo purpose. Amount is in paise
            PaymentIntent intent = stripeService.createPaymentIntent(
                    booking.getAmountPaise(),
                    "inr",
                    booking.getId().toString()
            );
            booking.setStripePaymentIntentId(intent.getId());
            booking.setStripeClientSecret(intent.getClientSecret());
            booking.setStatus(Booking.Status.PENDING_PAYMENT);
            bookingRepository.save(booking);

            return ResponseEntity.ok(Map.of(
                    "bookingId", booking.getId(),
                    "status", booking.getStatus().name(),
                    "paymentIntentId", intent.getId(),
                    "clientSecret", intent.getClientSecret()
            ));


        } catch (Throwable t) {
            t.printStackTrace();

            booking.setStatus(Booking.Status.FAILED);
            bookingRepository.save(booking);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", t.getClass().getName(),
                            "message",String.valueOf(t.getMessage())
                    )
            );

        }
    }
}
