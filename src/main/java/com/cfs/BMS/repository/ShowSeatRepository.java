package com.cfs.BMS.repository;

import com.cfs.BMS.entity.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

  List<ShowSeat> findByShowId(Long showId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.seat.id IN :seatIds")
  List<ShowSeat> lockByShowIdAndSeatIds(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId")
  List<ShowSeat> lockByShowId(@Param("showId") Long showId);
}
