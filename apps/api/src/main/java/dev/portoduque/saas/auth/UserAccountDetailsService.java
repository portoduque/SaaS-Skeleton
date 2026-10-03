package dev.portoduque.saas.auth;

import dev.portoduque.saas.users.UserAccount;
import dev.portoduque.saas.users.UserAccountRepository;
import java.util.List;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
final class UserAccountDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    UserAccountDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = users.findByEmail(AuthService.normalizeEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new User(user.getEmail(), user.getPasswordHash(), List.of());
    }
}
