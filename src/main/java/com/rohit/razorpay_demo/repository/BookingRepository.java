package com.rohit.razorpay_demo.repository;

import com.rohit.razorpay_demo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    Optional<Booking> findByRazorpayOrderId(String razorpayOrderId);
}
