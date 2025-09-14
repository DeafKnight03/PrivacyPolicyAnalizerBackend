package com.example.myapp.repository;

import com.example.myapp.entity.RefreshToken;
import com.example.myapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUserAndRevokedFalseAndExpiresAtAfter(User user, OffsetDateTime now);
}
