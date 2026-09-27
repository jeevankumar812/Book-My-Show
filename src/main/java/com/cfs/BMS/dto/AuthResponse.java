package com.cfs.BMS.dto;

import com.cfs.BMS.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class AuthResponse {
    private final String accessToken;
    private final String tokenType = "Bearer";
    private final Long userId;
    private final String email;
    private final UserRole role;
}
