package dev.portoduque.saas.auth;

import dev.portoduque.saas.shared.api.ApiPaths;
import dev.portoduque.saas.users.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
final class AuthController {

    private final AuthService authService;
    private final AuthSessionService sessions;

    AuthController(AuthService authService, AuthSessionService sessions) {
        this.authService = authService;
        this.sessions = sessions;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/registrations")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
        UserAccount user = authService.register(request.email(), request.password());
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/users/" + user.getId()))
                .body(UserResponse.from(user));
    }

    @PostMapping("/sessions")
    UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        return UserResponse.from(
                sessions.login(request.email(), request.password(), servletRequest, servletResponse));
    }

    @GetMapping("/session")
    UserResponse current(Authentication authentication) {
        return UserResponse.from(sessions.current(authentication));
    }

    @DeleteMapping("/session")
    ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        sessions.logout(authentication, request, response);
        return ResponseEntity.noContent().build();
    }

    record RegistrationRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 12, max = 128) String password) {}

    record LoginRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(max = 128) String password) {}

    record UserResponse(UUID id, String email, boolean emailVerified) {
        static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getEmail(), user.isEmailVerified());
        }
    }

    record CsrfResponse(
            @Schema(example = "X-CSRF-TOKEN") String headerName,
            @Schema(description = "Per-session CSRF token") String token) {}
}
