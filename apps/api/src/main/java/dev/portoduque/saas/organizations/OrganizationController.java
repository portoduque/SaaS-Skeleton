package dev.portoduque.saas.organizations;

import dev.portoduque.saas.shared.api.ApiPaths;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiPaths.V1)
class OrganizationController {

    static final String SELECTED_ORGANIZATION_ID = "selectedOrganizationId";

    private final OrganizationService organizations;

    OrganizationController(OrganizationService organizations) {
        this.organizations = organizations;
    }

    @PostMapping("/organizations")
    ResponseEntity<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request, Authentication authentication) {
        OrganizationMembership membership = organizations.create(request.name(), authentication);
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/organizations/" + membership.organization().id()))
                .body(OrganizationResponse.from(membership));
    }

    @GetMapping("/organizations")
    PageResponse<OrganizationResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(organizations.list(authentication, page, size), OrganizationResponse::from);
    }

    @GetMapping("/organizations/{organizationId}")
    OrganizationResponse get(@PathVariable UUID organizationId, Authentication authentication) {
        return OrganizationResponse.from(organizations.get(organizationId, authentication));
    }

    @GetMapping("/organizations/{organizationId}/memberships")
    PageResponse<MembershipResponse> listMemberships(
            @PathVariable UUID organizationId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(
                organizations.listMemberships(organizationId, authentication, page, size),
                MembershipResponse::from);
    }

    @PostMapping("/organizations/{organizationId}/memberships")
    ResponseEntity<MembershipResponse> addMembership(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AddMembershipRequest request,
            Authentication authentication) {
        OrganizationMembership membership = organizations.addMembership(
                organizationId, request.email(), request.role(), authentication);
        return ResponseEntity.created(URI.create(ApiPaths.V1 + "/organizations/" + organizationId
                        + "/memberships/" + membership.id()))
                .body(MembershipResponse.from(membership));
    }

    @PatchMapping("/organizations/{organizationId}/memberships/{membershipId}")
    MembershipResponse changeRole(
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody ChangeRoleRequest request,
            Authentication authentication) {
        return MembershipResponse.from(
                organizations.changeRole(organizationId, membershipId, request.role(), authentication));
    }

    @DeleteMapping("/organizations/{organizationId}/memberships/{membershipId}")
    ResponseEntity<Void> removeMembership(
            @PathVariable UUID organizationId,
            @PathVariable UUID membershipId,
            Authentication authentication) {
        organizations.removeMembership(organizationId, membershipId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/organization-selection")
    SelectionResponse select(
            @Valid @RequestBody SelectOrganizationRequest request,
            Authentication authentication,
            HttpSession session) {
        OrganizationMembership membership = organizations.select(request.organizationId(), authentication);
        session.setAttribute(SELECTED_ORGANIZATION_ID, membership.organization().id());
        return SelectionResponse.from(membership);
    }

    @GetMapping("/organization-selection")
    SelectionResponse selection(Authentication authentication, HttpSession session) {
        Object selected = session.getAttribute(SELECTED_ORGANIZATION_ID);
        if (!(selected instanceof UUID organizationId)) {
            throw new dev.portoduque.saas.shared.api.ApiProblemException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "NO_ORGANIZATION_SELECTED",
                    "No organization selected",
                    "No organization is selected for this session.");
        }
        return SelectionResponse.from(organizations.select(organizationId, authentication));
    }

    record CreateOrganizationRequest(@NotBlank @Size(min = 2, max = 100) String name) {}

    record AddMembershipRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotNull OrganizationRole role) {}

    record ChangeRoleRequest(@NotNull OrganizationRole role) {}

    record SelectOrganizationRequest(@NotNull UUID organizationId) {}

    record OrganizationResponse(UUID id, String name, OrganizationRole role) {
        static OrganizationResponse from(OrganizationMembership membership) {
            return new OrganizationResponse(
                    membership.organization().id(), membership.organization().name(), membership.role());
        }
    }

    record MembershipResponse(UUID id, UUID userId, String email, OrganizationRole role) {
        static MembershipResponse from(OrganizationMembership membership) {
            return new MembershipResponse(
                    membership.id(),
                    membership.user().getId(),
                    membership.user().getEmail(),
                    membership.role());
        }
    }

    record SelectionResponse(UUID organizationId, String name, OrganizationRole role) {
        static SelectionResponse from(OrganizationMembership membership) {
            return new SelectionResponse(
                    membership.organization().id(), membership.organization().name(), membership.role());
        }
    }

    record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        static <S, T> PageResponse<T> from(Page<S> source, java.util.function.Function<S, T> mapper) {
            return new PageResponse<>(
                    source.getContent().stream().map(mapper).toList(),
                    source.getNumber(),
                    source.getSize(),
                    source.getTotalElements(),
                    source.getTotalPages());
        }
    }
}
