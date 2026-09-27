package com.cfs.BMS;

import com.cfs.BMS.dto.BookingRequest;
import com.cfs.BMS.dto.UserRequest;
import com.cfs.BMS.entity.*;
import com.cfs.BMS.enums.SeatHoldStatus;
import com.cfs.BMS.enums.SeatType;
import com.cfs.BMS.exception.SeatAlreadyBookedException;
import com.cfs.BMS.repository.*;
import com.cfs.BMS.service.BookingService;
import com.cfs.BMS.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingConcurrencyTest {

    @Autowired BookingService bookingService;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired CityRepository cityRepository;
    @Autowired TheaterRepository theaterRepository;
    @Autowired ScreenRepository screenRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired ShowRepository showRepository;
    @Autowired ShowSeatRepository showSeatRepository;
    @Autowired BookingRepository bookingRepository;

    @Test
    void concurrentBookingSameSeat_onlyOneWins() throws Exception {
        User user = userService.register(new UserRequest("A", "a+" + UUID.randomUUID() + "@x.com", "password123", "999"));
        User user2 = userService.register(new UserRequest("B", "b+" + UUID.randomUUID() + "@x.com", "password123", "998"));
        City city = cityRepository.save(City.builder().name("C" + UUID.randomUUID()).state("S").build());
        Theater theater = theaterRepository.save(Theater.builder().name("T").address("A").city(city).build());
        Screen screen = screenRepository.save(Screen.builder().name("S1").totalSeats(10).theater(theater).build());
        Seat seat = seatRepository.save(Seat.builder().seatNumber("A1").row("A").col(1).seatType(SeatType.REGULAR).screen(screen).build());
        Movie movie = movieRepository.save(Movie.builder().title("M").genre("Action").language("Hindi").durationMinutes(120).build());
        Show show = showRepository.save(Show.builder().movie(movie).screen(screen)
                .showDate(LocalDate.now()).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .ticketPrice(250.0).build());
        showSeatRepository.save(ShowSeat.builder().show(show).seat(seat).status(SeatHoldStatus.AVAILABLE).build());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        Callable<Void> task = () -> null;
        Future<?> f1 = pool.submit(() -> {
            start.await();
            try {
                bookingService.createBooking(new BookingRequest(
                        user.getId(), show.getId(), List.of(seat.getId()), UUID.randomUUID()));
                wins.incrementAndGet();
            } catch (SeatAlreadyBookedException e) { conflicts.incrementAndGet(); }
            return null;
        });
        Future<?> f2 = pool.submit(() -> {
            start.await();
            try {
                bookingService.createBooking(new BookingRequest(
                        user2.getId(), show.getId(), List.of(seat.getId()), UUID.randomUUID()));
                wins.incrementAndGet();
            } catch (SeatAlreadyBookedException e) { conflicts.incrementAndGet(); }
            return null;
        });
        start.countDown();
        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, wins.get(), "exactly one booking must win");
        assertEquals(1, conflicts.get(), "other must get 409 conflict");
    }

    @Test
    void idempotentRetry_returnsSameBooking() {
        User user = userService.register(new UserRequest("C", "c+" + UUID.randomUUID() + "@x.com", "password123", "997"));
        City city = cityRepository.save(City.builder().name("C" + UUID.randomUUID()).state("S").build());
        Theater theater = theaterRepository.save(Theater.builder().name("T2").address("A").city(city).build());
        Screen screen = screenRepository.save(Screen.builder().name("S2").totalSeats(10).theater(theater).build());
        Seat seat = seatRepository.save(Seat.builder().seatNumber("A1").row("A").col(1).seatType(SeatType.REGULAR).screen(screen).build());
        Movie movie = movieRepository.save(Movie.builder().title("M2").genre("Action").language("Hindi").durationMinutes(120).build());
        Show show = showRepository.save(Show.builder().movie(movie).screen(screen)
                .showDate(LocalDate.now()).startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .ticketPrice(300.0).build());

        UUID key = UUID.randomUUID();
        var b1 = bookingService.createBooking(new BookingRequest(user.getId(), show.getId(), List.of(seat.getId()), key));
        var b2 = bookingService.createBooking(new BookingRequest(user.getId(), show.getId(), List.of(seat.getId()), key));
        assertEquals(b1.getId(), b2.getId());
        assertEquals(1, bookingRepository.findAll().size());
    }
}
