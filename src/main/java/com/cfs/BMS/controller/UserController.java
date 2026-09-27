package com.cfs.BMS.controller;


import com.cfs.BMS.dto.AuthResponse;
import com.cfs.BMS.dto.LoginRequest;
import com.cfs.BMS.dto.UserRequest;
import com.cfs.BMS.entity.User;
import com.cfs.BMS.repository.UserRepository;
import com.cfs.BMS.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/users", "/api/v1/users"})
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for user registration, authentication, and profiles")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public record UserResponse(Long id, String name, String email, String phone, String role) {
        static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                    u.getRole() == null ? null : u.getRole().name());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(userService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "User login — returns JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim()).orElse(null);
        return ResponseEntity.ok(new AuthResponse(token,
                user == null ? null : user.getId(), request.getEmail(),
                user == null ? com.cfs.BMS.enums.UserRole.USER : user.getRole()));
    }

    @GetMapping
    @Operation(summary = "Get all users (paginated, admin)")
    public ResponseEntity<Page<UserResponse>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable).map(UserResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (no password)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(userService.getUserById(id)));
    }
}
