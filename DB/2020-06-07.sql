update workflow_statuses set name='Disbursed',display_name='Disbursed' where internal_status='CLOSED' and workflow_id = (select id from workflows where granter_id=11 and object='DISBURSEMENT');


----

alter table actual_disbursements add column status boolean default false;