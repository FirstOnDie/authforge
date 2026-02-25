package com.authforge.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "appName", "TestApp");
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "appUrl", "http://localhost");
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "fromEmail", "test@test.local");
    }

    @Test
    void shouldSendVerificationEmail() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail("test@example.com", "token123");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldSendPasswordResetEmail() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail("test@example.com", "reset-token");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldThrowExceptionOnMailSendFailure() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(mimeMessage);

        assertThrows(com.authforge.exception.BadRequestException.class,
                () -> emailService.sendPasswordResetEmail("test@example.com", "reset-token"));
    }
}
