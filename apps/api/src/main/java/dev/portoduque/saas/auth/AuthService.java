package dev.portoduque.saas.auth;

import dev.portoduque.saas.shared.api.ApiProblemException;
import dev.portoduque.saas.users.UserAccount;
import dev.portoduque.saas.users.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);
    private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(15);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository users;
    private final AuthTokenRepository tokens;
    private final AuthRateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final JdbcIndexedSessionRepository sessions;

    AuthService(
            UserAccountRepository users,
            AuthTokenRepository tokens,
            AuthRateLimiter rateLimiter,
            PasswordEncoder passwordEncoder,
            Clock clock,
            JdbcIndexedSessionRepository sessions) {
        this.users = users;
        this.tokens = tokens;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.sessions = sessions;
    }

    @Transactional
    public UserAccount register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        rateLimiter.check(AuthAction.REGISTRATION, normalizedEmail, 3, REGISTRATION_WINDOW);
        if (users.existsByEmail(normalizedEmail)) {
            throw emailAlreadyRegistered();
        }
        Instant now = clock.instant();
        try {
            UserAccount user = users.saveAndFlush(
                    new UserAccount(normalizedEmail, passwordEncoder.encode(password), now));
            issueToken(user, AuthTokenType.EMAIL_VERIFICATION, EMAIL_VERIFICATION_TTL, now);
            return user;
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyRegistered();
        }
    }

    @Transactional
    IssuedAuthToken issueEmailVerification(UUID userId) {
        UserAccount user = users.findById(userId).orElseThrow(this::invalidToken);
        Instant now = clock.instant();
        rateLimiter.check(AuthAction.EMAIL_VERIFICATION, user.getEmail(), 3, Duration.ofHours(1));
        return issueToken(user, AuthTokenType.EMAIL_VERIFICATION, EMAIL_VERIFICATION_TTL, now);
    }

    @Transactional
    void verifyEmail(String rawToken) {
        AuthToken token = consumeToken(rawToken, AuthTokenType.EMAIL_VERIFICATION);
        token.user().verifyEmail(clock.instant());
    }

    @Transactional
    IssuedAuthToken issuePasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        rateLimiter.check(AuthAction.PASSWORD_RESET, normalizedEmail, 3, Duration.ofHours(1));
        return users.findByEmail(normalizedEmail)
                .map(user -> issueToken(
                        user, AuthTokenType.PASSWORD_RESET, PASSWORD_RESET_TTL, clock.instant()))
                .orElse(null);
    }

    @Transactional
    void resetPassword(String rawToken, String newPassword) {
        AuthToken token = consumeToken(rawToken, AuthTokenType.PASSWORD_RESET);
        token.user().changePassword(passwordEncoder.encode(newPassword), clock.instant());
        sessions.findByPrincipalName(token.user().getEmail())
                .keySet()
                .forEach(sessions::deleteById);
    }

    static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private IssuedAuthToken issueToken(
            UserAccount user, AuthTokenType tokenType, Duration timeToLive, Instant now) {
        tokens.consumeActiveTokens(user.getId(), tokenType, now);
        byte[] secret = new byte[32];
        SECURE_RANDOM.nextBytes(secret);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        Instant expiresAt = now.plus(timeToLive);
        tokens.save(new AuthToken(user, tokenType, SecretHashing.sha256(value), expiresAt, now));
        return new IssuedAuthToken(value, expiresAt);
    }

    private AuthToken consumeToken(String rawToken, AuthTokenType tokenType) {
        Instant now = clock.instant();
        AuthToken token = tokens.findByTokenHashAndTokenType(SecretHashing.sha256(rawToken), tokenType)
                .orElseThrow(this::invalidToken);
        if (!token.isUsableAt(now)) {
            throw invalidToken();
        }
        token.consume(now);
        return token;
    }

    private ApiProblemException emailAlreadyRegistered() {
        return new ApiProblemException(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                "E-mail already registered",
                "An account already exists for this e-mail address.");
    }

    private ApiProblemException invalidToken() {
        return new ApiProblemException(
                HttpStatus.BAD_REQUEST,
                "INVALID_OR_EXPIRED_TOKEN",
                "Invalid or expired token",
                "The token is invalid, expired, or has already been used.");
    }
}
