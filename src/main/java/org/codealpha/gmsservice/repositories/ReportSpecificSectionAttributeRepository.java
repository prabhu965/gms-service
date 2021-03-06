package org.codealpha.gmsservice.repositories;

import org.codealpha.gmsservice.entities.ReportSpecificSection;
import org.codealpha.gmsservice.entities.ReportSpecificSectionAttribute;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReportSpecificSectionAttributeRepository extends CrudRepository<ReportSpecificSectionAttribute,Long> {

    @Query(value = "select case when max(attribute_order)+1 is null then 1 else max(attribute_order)+1 end from report_specific_section_attributes where granter_id=?1 and section_id=?2",nativeQuery = true)
    public int getNextAttributeOrder(Long granterId, Long sectionId);

    public List<ReportSpecificSectionAttribute> findBySection(ReportSpecificSection section);
}
