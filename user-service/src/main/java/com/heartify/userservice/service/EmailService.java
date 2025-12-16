package com.heartify.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String username, String otpCode) {
        try {
            String htmlContent = loadTemplate("templates/otp.html");
            htmlContent = htmlContent.replace("{{ username }}", username);
            htmlContent = htmlContent.replace("{{ otp }}", otpCode);

            sendHtmlEmail(to, "Verify Your Account", htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    public void sendPasswordRecoveryEmail(String to, String username, String newPassword) {
        try {
            String htmlContent = loadTemplate("templates/password_recovery.html");
            htmlContent = htmlContent.replace("{{ username }}", username);
            htmlContent = htmlContent.replace("{{ newPassword }}", newPassword);

            sendHtmlEmail(to, "Password Recovery - Heartify", htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password recovery email: " + e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML
        
        mailSender.send(message);
    }

    private String loadTemplate(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}