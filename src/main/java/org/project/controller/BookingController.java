package org.project.controller;

import org.project.model.Booking;
import org.project.model.BookingStatus;
import org.project.model.Room;
import org.project.model.UserRole;
import org.project.repository.BookingRepository;
import org.project.repository.RoomRepository;
import org.project.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingController(BookingRepository bookingRepository, RoomRepository roomRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/new")
    public String newBooking(@RequestParam(required = false) Long roomId,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Principal principal) {
        UserRole role = getUserRole(principal);
        Booking booking = new Booking();
        booking.setApplicantEmail(principal.getName());
        booking.setReservedBy(principal.getName());
        if (roomId != null) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid room id: " + roomId));
            if (role != UserRole.ADMIN && isBooked(room)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You can only apply for available rooms.");
                return "redirect:/rooms";
            }
            booking.setRoom(room);
        }

        model.addAttribute("booking", booking);
        model.addAttribute("rooms", getSelectableRooms(role));
        model.addAttribute("bookingDate", "");
        model.addAttribute("startTime", "");
        model.addAttribute("endTime", "");
        return "bookings/form";
    }

    @PostMapping("/save")
    public String saveBooking(@ModelAttribute("booking") Booking booking,
                              BindingResult bindingResult,
                              @RequestParam(required = false) String bookingDate,
                              @RequestParam(required = false) String startTime,
                              @RequestParam(required = false) String endTime,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              Principal principal,
                              Authentication authentication) {
        Long roomId = booking.getRoom() == null ? null : booking.getRoom().getId();
        UserRole role = getUserRole(principal);

        if (role == UserRole.STUDENT) {
            bindingResult.reject("notAllowed", "Ordinary students are not allowed to book rooms.");
        }

        if (isBlank(booking.getReservedBy())) {
            bindingResult.rejectValue("reservedBy", "required", "Reserved by is required");
        }

        if (isBlank(booking.getPurpose())) {
            bindingResult.rejectValue("purpose", "required", "Purpose is required");
        }

        String timeSlot = buildTimeSlot(bookingDate, startTime, endTime, bindingResult);
        booking.setTimeSlot(timeSlot);

        if (roomId == null) {
            bindingResult.rejectValue("room", "required", "Please select a room");
        } else {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid room id: " + roomId));
            booking.setRoom(room);

            if (role != UserRole.ADMIN && isBooked(room)) {
                bindingResult.rejectValue("room", "unavailable", "You can only apply for available rooms.");
            }

            if (!isBlank(timeSlot) && bookingRepository.existsByRoomIdAndTimeSlotIgnoreCaseAndStatus(roomId, timeSlot, BookingStatus.CONFIRMED)) {
                bindingResult.rejectValue("timeSlot", "duplicate", "This room is already booked for that time slot");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", getSelectableRooms(role));
            model.addAttribute("bookingDate", bookingDate);
            model.addAttribute("startTime", startTime);
            model.addAttribute("endTime", endTime);
            return "bookings/form";
        }

        booking.setApplicantEmail(principal.getName());
        if (role == UserRole.ADMIN || authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.PENDING);
        }
        bookingRepository.save(booking);
        redirectAttributes.addFlashAttribute("successMessage",
                booking.getStatus() == BookingStatus.CONFIRMED
                        ? "Booking created successfully."
                        : "Application submitted. An admin must confirm it before the room is booked.");
        return "redirect:/rooms";
    }

    private String buildTimeSlot(String bookingDate, String startTime, String endTime, BindingResult bindingResult) {
        if (isBlank(bookingDate) || isBlank(startTime) || isBlank(endTime)) {
            bindingResult.rejectValue("timeSlot", "required", "Please select a date, start time, and end time");
            return "";
        }

        try {
            LocalDate date = LocalDate.parse(bookingDate);
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);

            if (!end.isAfter(start)) {
                bindingResult.rejectValue("timeSlot", "invalid", "End time must be after start time");
                return "";
            }

            return date.format(DATE_FORMAT) + " " + start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            bindingResult.rejectValue("timeSlot", "invalid", "Please select a valid date and time");
            return "";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private UserRole getUserRole(Principal principal) {
        return userRepository.findByEmailIgnoreCase(principal.getName())
                .map(user -> user.getRole())
                .orElse(UserRole.STUDENT);
    }

    private List<Room> getSelectableRooms(UserRole role) {
        List<Room> rooms = roomRepository.findAll();
        if (role == UserRole.ADMIN) {
            return rooms;
        }
        return rooms.stream()
                .filter(room -> !isBooked(room))
                .toList();
    }

    private boolean isBooked(Room room) {
        return bookingRepository.existsByRoomIdAndStatus(room.getId(), BookingStatus.CONFIRMED);
    }

    @GetMapping("/{id}/delete")
    public String deleteBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookingRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking deleted successfully.");
        return "redirect:/rooms";
    }
}
