package com.healthcare.userservice.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Healthcare App - Email Verification");
            message.setText("Your OTP code is: " + otpCode + "\n\nThis code will expire in 5 minutes.");
            
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email");
        }
    }
}