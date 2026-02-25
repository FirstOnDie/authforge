package com.authforge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${authforge.app.name:AuthForge}")
    private String appName;

    @Value("${authforge.app.url:http://localhost:4000}")
    private String appUrl;

    @Value("${authforge.app.from-email:noreply@authforge.local}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String token) {
        String verifyUrl = appUrl + "?verify=" + token;
        String subject = appName + " — Verify Your Email";
        String body = buildHtmlEmail(
                "Verify Your Email",
                "Thank you for registering! Click the button below to verify your email address.",
                verifyUrl,
                "Verify Email");
        sendHtml(to, subject, body);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = appUrl + "?reset=" + token;
        String subject = appName + " — Password Reset";
        String body = buildHtmlEmail(
                "Reset Your Password",
                "We received a request to reset your password. Click the button below to proceed.",
                resetUrl,
                "Reset Password");
        sendHtml(to, subject, body);
    }

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildHtmlEmail(String title, String message, String actionUrl, String buttonText) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#0a0a1a;font-family:'Inter',sans-serif;">
                <div style="max-width:500px;margin:40px auto;background:#14142a;border-radius:16px;border:1px solid rgba(139,92,246,0.15);padding:40px;">
                    <h1 style="color:#8B5CF6;font-size:28px;margin:0 0 8px;">🔐 %s</h1>
                    <h2 style="color:#F1F5F9;font-size:20px;margin:0 0 20px;">%s</h2>
                    <p style="color:#94A3B8;font-size:15px;line-height:1.6;margin:0 0 30px;">%s</p>
                    <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#8B5CF6,#6D28D9);color:#fff;text-decoration:none;padding:14px 32px;border-radius:10px;font-weight:700;font-size:15px;">%s</a>
                    <p style="color:#64748B;font-size:12px;margin-top:30px;">If you didn't request this, please ignore this email.</p>
                </div>
                </body>
                </html>
                """
                .formatted(appName, title, message, actionUrl, buttonText);
    }
}
