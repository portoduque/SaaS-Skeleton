package dev.portoduque.saas.auth;

import java.time.Instant;

record IssuedAuthToken(String value, Instant expiresAt) {}
