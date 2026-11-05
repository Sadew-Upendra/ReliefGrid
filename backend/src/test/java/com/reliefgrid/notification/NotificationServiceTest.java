package com.reliefgrid.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.reliefgrid.user.Role;
import com.reliefgrid.user.User;
import com.reliefgrid.user.UserRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class NotificationServiceTest {
    private UserRepository users;
    private NotificationRepository repo;
    private JavaMailSender mailSender;
    private NotificationService service;

    @BeforeEach
    void setUp() throws Exception {
        users = mock(UserRepository.class);
        repo = mock(NotificationRepository.class);
        mailSender = mock(JavaMailSender.class);
        service = new NotificationService(users, repo, mailSender);
        setField("mailHost", "");
        setField("fromAddress", "no-reply@reliefgrid.local");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setField(String name, String value) throws Exception {
        Field f = NotificationService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    @Test
    void withNoSmtpConfiguredItRecordsAnUndeliveredNotificationInsteadOfThrowing() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("citizen@example.com");
        u.setRole(Role.CITIZEN);
        when(users.findById(u.getId())).thenReturn(Optional.of(u));

        service.notifyUser(u.getId(), "Subject", "Message body");

        verify(repo).save(argThat(n -> !n.isDelivered() && n.getUserId().equals(u.getId())));
        verifyNoInteractions(mailSender);
    }

    @Test
    void notifyingAnUnknownUserDoesNotThrowOrSaveAnything() {
        UUID unknown = UUID.randomUUID();
        when(users.findById(unknown)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.notifyUser(unknown, "Subject", "Message"));
        verify(repo, never()).save(any());
    }
}
