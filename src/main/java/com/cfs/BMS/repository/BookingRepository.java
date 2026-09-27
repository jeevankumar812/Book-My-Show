package com.cfs.BMS.repository;

import com.cfs.BMS.entity.Booking;
import com.cfs.BMS.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, Long> {

  Page<Booking> findByUserId(Long userId, Pageable pageable);

  List<Booking> findByShowId(Long showId);

  Optional<Booking> findByIdempotencyKey(UUID idempotencyKey);

  @Query("SELECT s.id FROM Booking b JOIN b.seats s WHERE b.show.id=:showId AND b.status IN :statuses")
  List<Long> findBookedSeatIdsByShowId(@Param("showId") Long showId,
                                       @Param("statuses") List<BookingStatus> statuses);

  default List<Long> findConfirmedSeatIdsByShowId(Long showId) {
    return findBookedSeatIdsByShowId(showId, List.of(BookingStatus.CONFIRMED, BookingStatus.HOLD));
  }
}
