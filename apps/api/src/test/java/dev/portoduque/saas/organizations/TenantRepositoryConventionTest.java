package dev.portoduque.saas.organizations;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TenantRepositoryConventionTest {

    @Test
    void tenant_repositories_do_not_expose_unscoped_crud_reads_or_deletes() {
        assertExposesOnly(OrganizationRepository.class, Set.of("save"));
        assertExposesOnly(
                OrganizationMembershipRepository.class,
                Set.of(
                        "save",
                        "saveAndFlush",
                        "delete",
                        "findForUser",
                        "findForUserInOrganization",
                        "findForOrganization",
                        "findByIdAndOrganizationId",
                        "existsByOrganizationIdAndUserId"));
    }

    private void assertExposesOnly(Class<?> repositoryType, Set<String> allowedMethods) {
        assertThat(Arrays.stream(repositoryType.getMethods()).map(Method::getName))
                .as("repository surface exposed by %s", repositoryType.getSimpleName())
                .containsExactlyInAnyOrderElementsOf(allowedMethods);
    }
}
