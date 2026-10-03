package dev.portoduque.saas.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthToken> findByTokenHashAndTokenType(String tokenHash, AuthTokenType tokenType);

    @Modifying
    @Query("""
            update AuthToken token
               set token.consumedAt = :now
             where token.user.id = :userId
               and token.tokenType = :tokenType
               and token.consumedAt is null
            """)
    void consumeActiveTokens(
            @Param("userId") UUID userId,
            @Param("tokenType") AuthTokenType tokenType,
            @Param("now") Instant now);
}
