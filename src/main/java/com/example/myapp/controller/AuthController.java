package com.example.myapp.controller;

import com.example.myapp.dto.AuthResponse;
import com.example.myapp.dto.LoginRequest;
import com.example.myapp.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")  // prefisso comune
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
/*
    // POST /api/auth/signup
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest req) {
        return ResponseEntity.ok(authService.signup(req));
    }
*/
    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
/*
    // POST /api/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        authService.logout(req);
        return ResponseEntity.noContent().build();
    }

 */

    @PostMapping("/ping")
    public ResponseEntity<String> ping() { return ResponseEntity.ok("ok"); }
}

