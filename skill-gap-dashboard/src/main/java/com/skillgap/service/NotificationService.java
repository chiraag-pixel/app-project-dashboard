package com.skillgap.service;

import com.skillgap.entity.Notification;
import com.skillgap.entity.User;
import com.skillgap.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Simple in-app notifications. */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User user, String title, String message) {
        notificationRepository.save(new Notification(user, title, message));
    }

    @Transactional(readOnly = true)
    public List<Notification> listFor(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notificationRepository.countByUserIdAndReadFlagFalse(user.getId());
    }

    @Transactional
    public void markAllRead(User user) {
        listFor(user).forEach(n -> {
            if (!n.isReadFlag()) {
                n.setReadFlag(true);
                notificationRepository.save(n);
            }
        });
    }
}
