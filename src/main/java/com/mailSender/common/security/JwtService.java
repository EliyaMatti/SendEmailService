package com.mailSender.common.security;

import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.ExcelmailProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile(ApplicationProfiles.API)
public class JwtService {

  private final ExcelmailProperties properties;

  public JwtService(ExcelmailProperties properties) {
    this.properties = properties;
  }

  public String createToken(AuthPrincipal principal) {
    Instant now = Instant.now();
    Instant exp = now.plusMillis(properties.getSecurity().getJwtExpirationMs());
    return Jwts.builder()
        .subject(principal.userId().toString())
        .claim("org", principal.organizationId().toString())
        .claim("email", principal.email())
        .claim("role", principal.role())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key())
        .compact();
  }

  public AuthPrincipal parse(String token) {
    Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    return new AuthPrincipal(
        UUID.fromString(claims.getSubject()),
        UUID.fromString(claims.get("org", String.class)),
        claims.get("email", String.class),
        claims.get("role", String.class));
  }

  private SecretKey key() {
    String secret = properties.getSecurity().getJwtSecret();
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException("APP_JWT_SECRET must be at least 32 characters.");
    }
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
