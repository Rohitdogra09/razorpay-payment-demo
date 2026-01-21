package com.rohit.razorpay_demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @Value("${stripe.publishable.key}")
    private String publishableKey;

    @GetMapping("/pay")
    public String payPage(Model model) {
        model.addAttribute("stripePublishableKey", publishableKey);
        return "pay";
    }
}
