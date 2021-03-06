package org.codealpha.gmsservice.repositories;

import java.util.List;
import org.codealpha.gmsservice.entities.Organization;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Developer code-alpha.org
 **/
@Repository
public interface OrganizationRepository extends CrudRepository<Organization, Long> {

  public Organization findByCode(String code);

  public Organization findByOrganizationTypeEquals(String type);

  @Query(value = "select * from organizations where organization_type='GRANTEE'",nativeQuery = true)
  public List<Organization> getGranteeOrgs();

  @Query(value = "select * from organizations where organization_type='GRANTER'",nativeQuery = true)
  public List<Organization> getGranterOrgs();

  public Organization findByNameAndOrganizationType(String name, String type);

  @Query(value = "select distinct B.* from grants A inner join organizations B on B.id=A.organization_id where A.grantor_org_id=?1 order by B.name",nativeQuery = true)
  public List<Organization> getAssociatedGranteesForTenant(Long granterId);


    @Query(value="select * from organizations where name=?1",nativeQuery = true)
    Organization findByName(String grantee);
}
