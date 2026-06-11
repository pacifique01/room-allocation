package org.project.repository;

import org.project.model.Booking;
import org.project.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByRoomIdAndTimeSlotIgnoreCaseAndStatus(Long roomId, String timeSlot, BookingStatus status);

    boolean existsByRoomIdAndStatus(Long roomId, BookingStatus status);

    List<Booking> findByRoomIdOrderByIdDesc(Long roomId);

    List<Booking> findByStatusOrderByIdDesc(BookingStatus status);

    List<Booking> findByApplicantEmailIgnoreCaseOrderByIdDesc(String applicantEmail);
}
