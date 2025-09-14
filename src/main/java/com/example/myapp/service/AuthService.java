package com.example.myapp.service;

import com.example.myapp.dto.AuthResponse;
import com.example.myapp.dto.LoginRequest;
import com.example.myapp.dto.RefreshRequest;
import com.example.myapp.entity.RefreshToken;
import com.example.myapp.entity.User;
import com.example.myapp.repository.RefreshTokenRepository;
import com.example.myapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.myapp.dto.SignupRequest;
import com.example.myapp.entity.Role;
import com.example.myapp.repository.RoleRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepo,
                       RoleRepository roleRepo,
                       RefreshTokenRepository refreshRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    /* ---------- SIGNUP (register + immediate login) ---------- */
    @Transactional
    public AuthResponse signup(SignupRequest req) {
        var username = req.username();
        var rawPassword = req.password();
        var requestedRole = "USER";

        // Basic validations (add Bean Validation if you prefer)
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        Role r = new Role();
        r.setName("USER");
        r.setId(Long.valueOf(1));
        // Create user
        var user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(r);
        user = userRepo.save(user);

        // Issue tokens (auto-login on signup)
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        saveRefreshToken(user, refresh);

        return new AuthResponse(access, refresh, user.getUsername(), user.getRole().getName());
    }
    /* -------- Login -------- */

    public AuthResponse login(LoginRequest req) {
        //System.out.println("\nDIOCAN"+userRepo.findAll().get(0).getUsername()+"\t"+userRepo.findAll().get(0).getPasswordHash());
        User u = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        //System.out.println("DIOCAN"+passwordEncoder.matches(,u.getPasswordHash())  +"\t"+req.password());
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new BadCredentialsException("Bad credentials");
        }

        String access = jwtService.generateAccessToken(u);
        String refresh = jwtService.generateRefreshToken(u);

        // Persist hashed refresh for revocation/rotation
        saveRefreshToken(u, refresh);

        return new AuthResponse(access, refresh, u.getUsername(), u.getRole().getName());
    }

    /* -------- Refresh (rotation) -------- */

    public AuthResponse refresh(RefreshRequest req) {
        String rawRefresh = req.refreshToken();
        String subject = jwtService.extractUsername(rawRefresh); // throws if invalid signature
        User u = userRepo.findByUsername(subject)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check token integrity/expiry
        if (jwtService.isExpired(rawRefresh)) {
            throw new RuntimeException("Refresh token expired");
        }

        // Validate against DB (exists, not revoked, not expired)
        var hash = hashRefresh(rawRefresh);
        var existing = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Refresh token not recognized"));

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Refresh token invalid");
        }

        // ROTATION: revoke old + issue a new refresh
        existing.setRevoked(true);
        refreshRepo.save(existing);

        String newAccess = jwtService.generateAccessToken(u);
        String newRefresh = jwtService.generateRefreshToken(u);
        saveRefreshToken(u, newRefresh);

        return new AuthResponse(newAccess, newRefresh, u.getUsername(), u.getRole().getName());
    }

    /* -------- Logout (revoke) -------- */

    public void logout(RefreshRequest req) {
        String hash = hashRefresh(req.refreshToken());
        refreshRepo.findByTokenHash(hash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshRepo.save(rt);
        });
    }

    /* -------- Helpers -------- */

    private void saveRefreshToken(User u, String rawRefresh) {
        var rt = new RefreshToken();
        rt.setUser(u);
        rt.setTokenHash(hashRefresh(rawRefresh));
        rt.setIssuedAt(OffsetDateTime.now());

        // expiresAt = now + configured refresh days (reuse JwtService config by reading from token)
        // Simpler: extract exp from JWT so DB record matches token precisely
        var exp = jwtService.extractAllClaims(rawRefresh).getExpiration().toInstant();
        rt.setExpiresAt(OffsetDateTime.ofInstant(exp, OffsetDateTime.now().getOffset()));

        rt.setRevoked(false);
        refreshRepo.save(rt);
    }

    private String hashRefresh(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Cannot hash refresh token", e);
        }
    }
}
