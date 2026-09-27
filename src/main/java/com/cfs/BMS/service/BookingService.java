package com.cfs.BMS.service;


import com.cfs.BMS.dto.BookingRequest;
import com.cfs.BMS.entity.*;
import com.cfs.BMS.enums.BookingStatus;
import com.cfs.BMS.enums.SeatHoldStatus;
import com.cfs.BMS.exception.ConflictException;
import com.cfs.BMS.exception.NotFoundException;
import com.cfs.BMS.exception.SeatAlreadyBookedException;
import com.cfs.BMS.repository.BookingRepository;
import com.cfs.BMS.repository.SeatRepository;
import com.cfs.BMS.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserService userService;
    private final ShowService showService;

    /**
     * Concurrency-safe booking:
     * 1. Idempotency short-circuit (safe retries).
     * 2. PESSIMISTIC_WRITE lock on show_seats rows for (showId, seatIds).
     * 3. Validate seat belongs to show's screen + status AVAILABLE.
     * 4. Mark BOOKED + persist Booking in same TX. Unique (show_id,seat_id)
     *    + idempotency_key guard rails at DB level.
     */
    @Transactional
    public Booking createBooking(BookingRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty())
            throw new IllegalArgumentException("seatIds must not be empty");
        Set<Long> distinct = new HashSet<>(request.getSeatIds());
        if (distinct.size() != request.getSeatIds().size())
            throw new IllegalArgumentException("Duplicate seatIds in request");

        UUID key = request.getIdempotencyKey();
        if (key != null) {
            var existing = bookingRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) return existing.get();
        }

        User user = userService.getUserById(request.getUserId());
        Show show = showService.getShowById(request.getShowId());
        List<Long> seatIds = List.copyOf(distinct);

        // Fast path: legacy bookings (pre-ShowSeat rows) — check confirmed seats.
        List<ShowSeat> locked = showSeatRepository.lockByShowIdAndSeatIds(show.getId(), seatIds);

        List<Seat> seats;
        BigDecimal total;
        if (locked.isEmpty()) {
            // No inventory rows (legacy data): fall back to booking_seats scan + validate.
            List<Long> taken = bookingRepository.findConfirmedSeatIdsByShowId(show.getId());
            for (Long sid : seatIds)
                if (taken.contains(sid)) throw new SeatAlreadyBookedException("Seat with id " + sid + " is already booked");
            seats = seatRepository.findAllById(seatIds);
            if (seats.size() != seatIds.size()) throw new NotFoundException("Some seats are invalid");
            validateSameScreen(show, seats);
            total = show.getTicketPrice() == null ? BigDecimal.ZERO
                    : BigDecimal.valueOf(show.getTicketPrice()).multiply(BigDecimal.valueOf(seats.size()));
            Booking booking = Booking.builder()
                    .user(user).show(show).seats(seats)
                    .totalPrice(total).status(BookingStatus.CONFIRMED)
                    .idempotencyKey(key != null ? key : UUID.randomUUID())
                    .build();
            try {
                return bookingRepository.saveAndFlush(booking);
            } catch (DataIntegrityViolationException e) {
                throw new ConflictException("Booking conflict — retry with a new idempotency key");
            }
        }

        if (locked.size() != seatIds.size())
            throw new NotFoundException("Some seats are invalid for this show (missing inventory rows)");
        for (ShowSeat ss : locked) {
            if (ss.getStatus() != SeatHoldStatus.AVAILABLE)
                throw new SeatAlreadyBookedException("Seat with id " + ss.getSeat().getId() + " is already booked");
            if (!ss.getSeat().getScreen().getId().equals(show.getScreen().getId()))
                throw new IllegalArgumentException("Seat " + ss.getSeat().getId() + " does not belong to this show's screen");
        }
        locked.forEach(ss -> ss.setStatus(SeatHoldStatus.BOOKED));
        showSeatRepository.saveAll(locked);

        seats = locked.stream().map(ShowSeat::getSeat).toList();
        total = show.getTicketPrice() == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(show.getTicketPrice()).multiply(BigDecimal.valueOf(seats.size()));

        Booking booking = Booking.builder()
                .user(user).show(show).seats(new ArrayList<>(seats))
                .totalPrice(total).status(BookingStatus.CONFIRMED)
                .idempotencyKey(key != null ? key : UUID.randomUUID())
                .build();
        try {
            return bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            throw new SeatAlreadyBookedException("Seats were just booked by another request — please pick others");
        }
    }

    private void validateSameScreen(Show show, List<Seat> seats) {
        Long screenId = show.getScreen().getId();
        for (Seat s : seats)
            if (s.getScreen() == null || !screenId.equals(s.getScreen().getId()))
                throw new IllegalArgumentException("Seat " + s.getId() + " does not belong to this show's screen");
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));
        b.getSeats().size();
        return b;
    }

    public Page<Booking> getBookingByUser(Long userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable);
    }

    /** Legacy non-paged variant. */
    public List<Booking> getBookingByUser(Long userId) {
        return getBookingByUser(userId, Pageable.unpaged()).getContent();
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) return booking;
        booking.setStatus(BookingStatus.CANCELLED);
        // Release inventory rows so seats become re-bookable.
        List<Long> seatIds = booking.getSeats().stream().map(Seat::getId).toList();
        if (!seatIds.isEmpty()) {
            var rows = showSeatRepository.lockByShowIdAndSeatIds(booking.getShow().getId(), seatIds);
            rows.forEach(r -> r.setStatus(SeatHoldStatus.AVAILABLE));
            showSeatRepository.saveAll(rows);
        }
        return bookingRepository.save(booking);
    }

    /** Back-compat alias (old typo). */
    @Transactional
    public Booking cancelbooking(Long bookingId) {
        return cancelBooking(bookingId);
    }

    @Transactional(readOnly = true)
    public List<Seat> getAvailableSeats(Long showId) {
        Show show = showService.getShowById(showId);
        List<Seat> allSeats = seatRepository.findByScreenId(show.getScreen().getId());
        // Prefer inventory table when populated.
        List<ShowSeat> rows = showSeatRepository.findByShowId(showId);
        if (!rows.isEmpty()) {
            Set<Long> blocked = new HashSet<>();
            rows.forEach(r -> { if (r.getStatus() != SeatHoldStatus.AVAILABLE) blocked.add(r.getSeat().getId()); });
            return allSeats.stream().filter(s -> !blocked.contains(s.getId())).toList();
        }
        List<Long> taken = bookingRepository.findConfirmedSeatIdsByShowId(showId);
        return allSeats.stream().filter(s -> !taken.contains(s.getId())).toList();
    }
}
