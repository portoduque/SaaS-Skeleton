package dev.portoduque.saas.organizations;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    @Query(
            value = """
                    select membership
                      from OrganizationMembership membership
                      join fetch membership.organization
                     where membership.user.id = :userId
                    """,
            countQuery = """
                    select count(membership)
                      from OrganizationMembership membership
                     where membership.user.id = :userId
                    """)
    Page<OrganizationMembership> findForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select membership
              from OrganizationMembership membership
              join fetch membership.organization
             where membership.organization.id = :organizationId
               and membership.user.id = :userId
            """)
    Optional<OrganizationMembership> findForUserInOrganization(
            @Param("organizationId") UUID organizationId, @Param("userId") UUID userId);

    @Query(
            value = """
                    select membership
                      from OrganizationMembership membership
                      join fetch membership.user
                     where membership.organization.id = :organizationId
                    """,
            countQuery = """
                    select count(membership)
                      from OrganizationMembership membership
                     where membership.organization.id = :organizationId
                    """)
    Page<OrganizationMembership> findForOrganization(
            @Param("organizationId") UUID organizationId, Pageable pageable);

    @Query("""
            select membership
              from OrganizationMembership membership
              join fetch membership.organization
              join fetch membership.user
             where membership.id = :id
               and membership.organization.id = :organizationId
            """)
    Optional<OrganizationMembership> findByIdAndOrganizationId(
            @Param("id") UUID id, @Param("organizationId") UUID organizationId);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
