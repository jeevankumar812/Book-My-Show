package com.cfs.BMS.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long showId;

    @NotEmpty
    private List<Long> seatIds;

    /** Client-generated key for safe retries. Optional — server generates if absent. */
    private UUID idempotencyKey;
}
