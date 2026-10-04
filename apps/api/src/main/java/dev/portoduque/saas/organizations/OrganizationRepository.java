package dev.portoduque.saas.organizations;

import java.util.UUID;
import org.springframework.data.repository.Repository;

interface OrganizationRepository extends Repository<Organization, UUID> {

    Organization save(Organization organization);
}
