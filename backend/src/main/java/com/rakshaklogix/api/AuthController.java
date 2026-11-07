package com.rakshaklogix.api;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final SecretKey key;
    private final long expiration;

    public AuthController(JdbcTemplate db, PasswordEncoder encoder,
            @Value("${jwt.secret}") String secret, @Value("${jwt.expirationMs}") long expiration) {
        this.db = db; this.encoder = encoder; this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expiration = expiration;
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        var rows = db.query("SELECT id, email, password, full_name, role FROM users WHERE email=? AND enabled=TRUE", (rs, n) ->
                Map.of("id", rs.getLong("id"), "email", rs.getString("email"), "password", rs.getString("password"),
                        "name", rs.getString("full_name"), "role", rs.getString("role")), request.email());
        if (rows.isEmpty() || !encoder.matches(request.password(), (String) rows.getFirst().get("password")))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        var user = rows.getFirst();
        Instant now = Instant.now();
        String token = Jwts.builder().subject(request.email()).claim("role", user.get("role"))
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expiration))).signWith(key).compact();
        return Map.of("token", token, "user", Map.of("id", user.get("id"), "email", user.get("email"), "name", user.get("name"), "role", user.get("role")));
    }

    @GetMapping("/me")
    public Map<String, Object> me(org.springframework.security.core.Authentication auth) {
        return db.queryForMap("SELECT id, email, full_name AS name, role FROM users WHERE email=?", auth.getName());
    }
}
