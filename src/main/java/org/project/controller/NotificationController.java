package org.project.controller;

import org.project.repository.NotificationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/notifications")
    public String notifications(Model model, Principal principal) {
        model.addAttribute("notifications",
                notificationRepository.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(principal.getName()));
        return "notifications/list";
    }
}
