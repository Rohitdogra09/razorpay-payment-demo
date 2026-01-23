package com.rohit.razorpay_demo.controller;

import com.rohit.razorpay_demo.entity.Booking;
import com.rohit.razorpay_demo.repository.BookingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;

    public BookingController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> body) {

        String productName = (String) body.get("productName");
        Number amountRupees = (Number) body.get("amountRupees");

        if (productName == null || productName.isBlank() || amountRupees == null) {
            return ResponseEntity.badRequest().body("productName and amountRupees are required");
        }

        long amountPaise = amountRupees.longValue() * 100;

        Booking booking = new Booking();
        booking.setProductName(productName);
        booking.setAmountPaise(amountPaise);
        booking.setStatus(Booking.Status.CREATED);

        Booking saved = bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of(
                "bookingId", saved.getId(),
                "status", saved.getStatus().name(),
                "amountPaise", saved.getAmountPaise()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .<ResponseEntity<?>>map(b -> {
                    java.util.Map<String, Object> resp = new java.util.HashMap<>();
                    resp.put("id", b.getId());
                    resp.put("productName", b.getProductName());
                    resp.put("amountPaise", b.getAmountPaise());
                    resp.put("status", b.getStatus() == null ? null : b.getStatus().name());
                    resp.put("razorpayOrderId", b.getRazorpayOrderId());
                    resp.put("razorpayPaymentId", b.getRazorpayPaymentId());
                    resp.put("stripePaymentIntentId", b.getStripePaymentIntentId());
                    resp.put("stripeClientSecret", b.getStripeClientSecret());
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
