package dev.portoduque.saas.organizations;

import dev.portoduque.saas.shared.api.ApiProblemException;
import dev.portoduque.saas.users.UserAccount;
import dev.portoduque.saas.users.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;
    private final UserAccountRepository users;
    private final Clock clock;

    OrganizationService(
            OrganizationRepository organizations,
            OrganizationMembershipRepository memberships,
            UserAccountRepository users,
            Clock clock) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    OrganizationMembership create(String name, Authentication authentication) {
        UserAccount user = currentUser(authentication);
        Instant now = clock.instant();
        Organization organization = organizations.save(new Organization(name.strip(), now));
        return memberships.save(new OrganizationMembership(organization, user, OrganizationRole.OWNER, now));
    }

    @Transactional(readOnly = true)
    Page<OrganizationMembership> list(Authentication authentication, int page, int size) {
        return memberships.findForUser(
                currentUser(authentication).getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "organization.name")
                        .and(Sort.by(Sort.Direction.ASC, "organization.id"))));
    }

    @Transactional(readOnly = true)
    OrganizationMembership get(UUID organizationId, Authentication authentication) {
        return requireMembership(organizationId, currentUser(authentication).getId());
    }

    @Transactional(readOnly = true)
    Page<OrganizationMembership> listMemberships(
            UUID organizationId, Authentication authentication, int page, int size) {
        UserAccount current = currentUser(authentication);
        requireMembership(organizationId, current.getId());
        return memberships.findForOrganization(
                organizationId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "user.email")
                        .and(Sort.by(Sort.Direction.ASC, "id"))));
    }

    @Transactional
    OrganizationMembership addMembership(
            UUID organizationId, String email, OrganizationRole role, Authentication authentication) {
        requireOwner(organizationId, authentication);
        if (role == OrganizationRole.OWNER) {
            throw ownerRoleReserved();
        }
        UserAccount user = users.findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(this::userNotFound);
        if (memberships.existsByOrganizationIdAndUserId(organizationId, user.getId())) {
            throw membershipAlreadyExists();
        }
        Organization organization = organizations.findById(organizationId).orElseThrow(this::organizationNotFound);
        try {
            return memberships.saveAndFlush(
                    new OrganizationMembership(organization, user, role, clock.instant()));
        } catch (DataIntegrityViolationException exception) {
            throw membershipAlreadyExists();
        }
    }

    @Transactional
    OrganizationMembership changeRole(
            UUID organizationId,
            UUID membershipId,
            OrganizationRole role,
            Authentication authentication) {
        requireOwner(organizationId, authentication);
        OrganizationMembership membership = requireTargetMembership(organizationId, membershipId);
        requireMutableMembership(membership);
        if (role == OrganizationRole.OWNER) {
            throw ownerRoleReserved();
        }
        membership.changeRole(role, clock.instant());
        return membership;
    }

    @Transactional
    void removeMembership(UUID organizationId, UUID membershipId, Authentication authentication) {
        requireOwner(organizationId, authentication);
        OrganizationMembership membership = requireTargetMembership(organizationId, membershipId);
        requireMutableMembership(membership);
        memberships.delete(membership);
    }

    @Transactional(readOnly = true)
    OrganizationMembership select(UUID organizationId, Authentication authentication) {
        return requireMembership(organizationId, currentUser(authentication).getId());
    }

    private UserAccount currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw unauthenticated();
        }
        return users.findByEmail(authentication.getName()).orElseThrow(this::unauthenticated);
    }

    private OrganizationMembership requireOwner(UUID organizationId, Authentication authentication) {
        OrganizationMembership membership = requireMembership(
                organizationId, currentUser(authentication).getId());
        if (membership.role() != OrganizationRole.OWNER) {
            throw new ApiProblemException(
                    HttpStatus.FORBIDDEN,
                    "OWNER_ROLE_REQUIRED",
                    "Owner role required",
                    "Only an organization owner may perform this operation.");
        }
        return membership;
    }

    private OrganizationMembership requireMembership(UUID organizationId, UUID userId) {
        return memberships.findForUserInOrganization(organizationId, userId)
                .orElseThrow(this::organizationNotFound);
    }

    private OrganizationMembership requireTargetMembership(UUID organizationId, UUID membershipId) {
        return memberships.findByIdAndOrganizationId(membershipId, organizationId)
                .orElseThrow(() -> new ApiProblemException(
                        HttpStatus.NOT_FOUND,
                        "MEMBERSHIP_NOT_FOUND",
                        "Membership not found",
                        "The requested membership was not found."));
    }

    private void requireMutableMembership(OrganizationMembership membership) {
        if (membership.role() == OrganizationRole.OWNER) {
            throw new ApiProblemException(
                    HttpStatus.CONFLICT,
                    "OWNER_MEMBERSHIP_IMMUTABLE",
                    "Owner membership is immutable",
                    "The owner membership cannot be changed or removed.");
        }
    }

    private ApiProblemException organizationNotFound() {
        return new ApiProblemException(
                HttpStatus.NOT_FOUND,
                "ORGANIZATION_NOT_FOUND",
                "Organization not found",
                "The requested organization was not found.");
    }

    private ApiProblemException membershipAlreadyExists() {
        return new ApiProblemException(
                HttpStatus.CONFLICT,
                "MEMBERSHIP_ALREADY_EXISTS",
                "Membership already exists",
                "The user is already a member of this organization.");
    }

    private ApiProblemException ownerRoleReserved() {
        return new ApiProblemException(
                HttpStatus.BAD_REQUEST,
                "OWNER_ROLE_RESERVED",
                "Owner role is reserved",
                "The owner role is assigned only when an organization is created.");
    }

    private ApiProblemException userNotFound() {
        return new ApiProblemException(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "User not found",
                "No registered user was found for the supplied e-mail address.");
    }

    private ApiProblemException unauthenticated() {
        return new ApiProblemException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                "Authentication required",
                "Authentication is required to access this resource.");
    }
}
