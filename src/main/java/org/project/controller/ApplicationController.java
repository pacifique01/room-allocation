package org.project.controller;

import org.project.model.Booking;
import org.project.model.BookingStatus;
import org.project.repository.BookingRepository;
import org.project.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public ApplicationController(BookingRepository bookingRepository, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String pendingApplications(Model model) {
        model.addAttribute("applications", bookingRepository.findByStatusOrderByIdDesc(BookingStatus.PENDING));
        return "applications/list";
    }

    @PostMapping("/{id}/confirm")
    public String confirmApplication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid application id: " + id));

        if (bookingRepository.existsByRoomIdAndTimeSlotIgnoreCaseAndStatus(
                booking.getRoom().getId(),
                booking.getTimeSlot(),
                BookingStatus.CONFIRMED)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This room already has a confirmed booking for that time slot.");
            return "redirect:/applications";
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        boolean emailSent = notificationService.notifyUser(
                booking.getApplicantEmail(),
                "Room application confirmed",
                "Your application for " + booking.getRoom().getRoomNumber() + " at " + booking.getTimeSlot()
                        + " has been confirmed."
        );
        redirectAttributes.addFlashAttribute("successMessage", emailSent
                ? "Application confirmed. Email notification sent to " + booking.getApplicantEmail() + "."
                : "Application confirmed. In-app notification saved, but email could not be sent.");
        return "redirect:/applications";
    }

    @PostMapping("/{id}/reject")
    public String rejectApplication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid application id: " + id));
        booking.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(booking);
        boolean emailSent = notificationService.notifyUser(
                booking.getApplicantEmail(),
                "Room application rejected",
                "Your application for " + booking.getRoom().getRoomNumber() + " at " + booking.getTimeSlot()
                        + " was rejected."
        );
        redirectAttributes.addFlashAttribute("successMessage", emailSent
                ? "Application rejected. Email notification sent to " + booking.getApplicantEmail() + "."
                : "Application rejected. In-app notification saved, but email could not be sent.");
        return "redirect:/applications";
    }
}
