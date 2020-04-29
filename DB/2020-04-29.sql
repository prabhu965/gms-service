create materialized view granter_count_and_amount_totals  as select A.grantor_org_id granter_id,sum(amount) total_grant_amount,count(*) total_grants from grants A inner join workflow_statuses B on A.grant_status_id=B.id where ( B.internal_status='ACTIVE' or B.internal_status='CLOSED') group by A.grantor_org_id;
create materialized view granter_grantees as select distinct A.grantor_org_id granter_id,count( distinct A.organization_id) grantee_totals from grants A inner join workflow_statuses B on A.grant_status_id=B.id where ( B.internal_status='ACTIVE' or B.internal_status='CLOSED') group by A.grantor_org_id;
create materialized view granter_active_users as select B.id granter_id,count(*) active_users from users A inner join organizations B on B.id=A.organization_id where A.active=true and B.organization_type='GRANTER' group by B.id;

create or replace function refresh_mat_views()
returns void
as $BODY$
begin
    refresh materialized view granter_count_and_amount_totals;
	refresh materialized view granter_grantees;
end
$BODY$ LANGUAGE 'plpgsql';

CREATE OR REPLACE FUNCTION process_grant_state_change()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
    BEGIN
        IF (TG_OP = 'UPDATE' AND OLD.grant_status_id!=NEW.grant_status_id) THEN
            INSERT INTO grant_history (
id, amount, created_at, created_by, description, end_date, name, representative, start_date, status_name, template_id, updated_at, updated_by, grant_status_id, grantor_org_id, organization_id, substatus_id, note, note_added,note_added_by,moved_on) select OLD.*;

			select "refresh_mat_views"();

            RETURN NEW;
        END IF;
        RETURN NULL; -- result is ignored since this is an AFTER trigger
    END;
$BODY$;