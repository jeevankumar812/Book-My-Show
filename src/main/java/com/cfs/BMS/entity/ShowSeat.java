package com.cfs.BMS.entity;

import com.cfs.BMS.enums.SeatHoldStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "show_seats",
        uniqueConstraints = @UniqueConstraint(name = "uq_show_seats_show_seat", columnNames = {"show_id", "seat_id"}),
        indexes = @Index(name = "idx_show_seats_show_status", columnList = "show_id,status"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private SeatHoldStatus status = SeatHoldStatus.AVAILABLE;

    @Version
    private Long version;

    private Instant holdExpiresAt;
}
