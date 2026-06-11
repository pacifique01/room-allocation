package org.project.controller;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.project.service.MailConfigurationException;
import org.project.service.NotificationService;

@Controller
@RequestMapping("/admin/email")
public class MailTestController {

    private final NotificationService notificationService;

    public MailTestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String emailTest(Model model) {
        model.addAttribute("recipientEmail", "");
        return "admin/email-test";
    }

    @PostMapping("/test")
    public String sendTestEmail(@RequestParam String recipientEmail,
                                RedirectAttributes redirectAttributes) {
        try {
            notificationService.sendTestEmail(recipientEmail);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Test email sent to " + recipientEmail + ".");
        } catch (MailAuthenticationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Gmail rejected the sender credentials. Use the 16-character Gmail App Password in MAIL_PASSWORD, then restart the app.");
        } catch (MailConfigurationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (MailException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Email could not be sent: " + ex.getMostSpecificCause().getMessage());
        }
        return "redirect:/admin/email";
    }
}
