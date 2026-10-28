package com.skillgap.controller;

import com.skillgap.entity.User;
import com.skillgap.security.CurrentUserService;
import com.skillgap.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Simple in-app notification center. */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public NotificationController(CurrentUserService currentUserService,
                                  NotificationService notificationService) {
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUserService.getUser();
        model.addAttribute("notifications", notificationService.listFor(user));
        return "notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead() {
        notificationService.markAllRead(currentUserService.getUser());
        return "redirect:/notifications";
    }
}
