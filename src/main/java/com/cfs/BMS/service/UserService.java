package com.cfs.BMS.service;

import com.cfs.BMS.dto.LoginRequest;
import com.cfs.BMS.dto.UserRequest;
import com.cfs.BMS.entity.User;
import com.cfs.BMS.exception.ConflictException;
import com.cfs.BMS.exception.NotFoundException;
import com.cfs.BMS.repository.UserRepository;
import com.cfs.BMS.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public User register(UserRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists: " + email);
        }
        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .build();
        return userRepository.save(user);
    }

    /** Returns JWT on success; never returns the User with password. */
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new NotFoundException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new NotFoundException("Invalid email or password");
        }
        return jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
