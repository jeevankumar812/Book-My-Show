package com.cfs.BMS.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "shows", indexes = {
        @Index(name = "idx_shows_movie_date", columnList = "movie_id,showDate"),
        @Index(name = "idx_shows_screen_date", columnList = "screen_id,showDate")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="movie_id",nullable = false)
    private Movie movie;

    @ManyToOne
    @JoinColumn(name="screen_id",nullable = false)
    private Screen screen;

    @Column(nullable = false)
    private LocalDate showDate;

    @Column(nullable = false)
    private LocalTime startTime;

    private LocalTime endTime;

    private Double ticketPrice;
}
