package org.codealpha.gmsservice.repositories;

import java.util.List;
import org.codealpha.gmsservice.entities.AppConfig;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AppConfigRepository extends CrudRepository<AppConfig,Long> {

  @Query(value = "select A.id,case when B.config_name is not null then B.config_name else A.config_name end as config_name,case when B.config_name is not null then B.config_value else A.config_value end as config_value from app_config A left join (select * from org_config where granter_id=?1) B on A.config_name=B.config_name",nativeQuery = true)
  public List<AppConfig> getAllAppConfigForOrg(Long grantorOrgId);

  @Query(value = "select distinct A.id,case when B.config_name is not null then B.config_name else A.config_name end as config_name,case when B.config_name is not null then B.config_value else A.config_value end as config_value from app_config A left join (select * from org_config where granter_id=?1) B on A.config_name=B.config_name where A.config_name=?2",nativeQuery = true)
  public AppConfig getAppConfigForOrg(Long grantorOrgId,String appConfigName);
}
