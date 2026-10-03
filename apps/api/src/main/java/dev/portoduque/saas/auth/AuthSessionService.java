package dev.portoduque.saas.auth;

import dev.portoduque.saas.shared.api.ApiProblemException;
import dev.portoduque.saas.users.UserAccount;
import dev.portoduque.saas.users.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
final class AuthSessionService {

    private static final int LOGIN_ATTEMPTS = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final UserAccountRepository users;
    private final AuthRateLimiter rateLimiter;

    AuthSessionService(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            UserAccountRepository users,
            AuthRateLimiter rateLimiter) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.users = users;
        this.rateLimiter = rateLimiter;
    }

    UserAccount login(
            String email, String password, HttpServletRequest request, HttpServletResponse response) {
        String normalizedEmail = AuthService.normalizeEmail(email);
        rateLimiter.check(AuthAction.LOGIN, normalizedEmail, LOGIN_ATTEMPTS, LOGIN_WINDOW);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, password));
            sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            rateLimiter.clear(AuthAction.LOGIN, normalizedEmail);
            return users.findByEmail(normalizedEmail).orElseThrow(this::invalidCredentials);
        } catch (BadCredentialsException exception) {
            throw invalidCredentials();
        }
    }

    UserAccount current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw unauthenticated();
        }
        return users.findByEmail(authentication.getName()).orElseThrow(this::unauthenticated);
    }

    void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    private ApiProblemException invalidCredentials() {
        return new ApiProblemException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid credentials",
                "The supplied credentials are invalid.");
    }

    private ApiProblemException unauthenticated() {
        return new ApiProblemException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                "Authentication required",
                "Authentication is required to access this resource.");
    }
}
