package dev.portoduque.saas.organizations;

import dev.portoduque.saas.shared.persistence.UuidV7;
import dev.portoduque.saas.users.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_memberships")
class OrganizationMembership {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrganizationRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationMembership() {}

    OrganizationMembership(Organization organization, UserAccount user, OrganizationRole role, Instant now) {
        this.id = UuidV7.generate();
        this.organization = organization;
        this.user = user;
        this.role = role;
        this.createdAt = now;
        this.updatedAt = now;
    }

    UUID id() {
        return id;
    }

    Organization organization() {
        return organization;
    }

    UserAccount user() {
        return user;
    }

    OrganizationRole role() {
        return role;
    }

    void changeRole(OrganizationRole newRole, Instant now) {
        role = newRole;
        updatedAt = now;
    }
}
