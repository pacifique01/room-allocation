package org.project.controller;

import jakarta.validation.Valid;
import org.project.model.BookingStatus;
import org.project.model.Room;
import org.project.repository.BookingRepository;
import org.project.repository.NotificationRepository;
import org.project.repository.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;

    public RoomController(RoomRepository roomRepository,
                          BookingRepository bookingRepository,
                          NotificationRepository notificationRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public String listRooms(Model model, Principal principal) {
        var rooms = roomRepository.findAll();
        model.addAttribute("rooms", rooms);
        model.addAttribute("availableRooms", rooms.stream()
                .filter(room -> room.getBookings().stream()
                        .noneMatch(booking -> booking.getStatus() == BookingStatus.CONFIRMED))
                .toList());
        model.addAttribute("bookedRooms", rooms.stream()
                .filter(room -> room.getBookings().stream()
                        .anyMatch(booking -> booking.getStatus() == BookingStatus.CONFIRMED))
                .toList());
        model.addAttribute("pendingApplications", bookingRepository.findByStatusOrderByIdDesc(BookingStatus.PENDING));
        model.addAttribute("myApplications", bookingRepository.findByApplicantEmailIgnoreCaseOrderByIdDesc(principal.getName()));
        model.addAttribute("notifications",
                notificationRepository.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(principal.getName()));
        return "rooms/list";
    }

    @GetMapping("/new")
    public String newRoom(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("pageTitle", "Add Room");
        return "rooms/form";
    }

    @GetMapping("/{id}/edit")
    public String editRoom(@PathVariable Long id, Model model) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid room id: " + id));
        model.addAttribute("room", room);
        model.addAttribute("pageTitle", "Edit Room");
        return "rooms/form";
    }

    @PostMapping("/save")
    public String saveRoom(@Valid @ModelAttribute("room") Room room,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (room.getId() == null && roomRepository.existsByRoomNumberIgnoreCase(room.getRoomNumber())) {
            bindingResult.rejectValue("roomNumber", "duplicate", "This room number already exists");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", room.getId() == null ? "Add Room" : "Edit Room");
            return "rooms/form";
        }

        roomRepository.save(room);
        redirectAttributes.addFlashAttribute("successMessage", "Room saved successfully.");
        return "redirect:/rooms";
    }

    @GetMapping("/{id}/delete")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        roomRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Room deleted successfully.");
        return "redirect:/rooms";
    }
}
