update app_config set config_value='{ "messageReport": "<p>The Report approval workflow for <b>%REPORT_NAME%</b> for <b>%GRANT_NAME%</b> requires your action.</p><p>This has been in your queue for %NO_DAYS% day(s)</p><p> Please log on to Anudan to progress the workflow. </p><p>This is a system generated reminder for <b>%TENANT%</b>. Please ignore this reminder if you have already actioned the workflow.</p>", "messageGrant": "<p>The Grant workflow for <b>%GRANT_NAME%</b> requires your action.</p><p>This has been in your queue for %NO_DAYS% day(s)</p><p> Please log on to Anudan to progress the workflow. </p><p>This is a system generated reminder for <b>%TENANT%</b>. Please ignore this reminder if you have already actioned the workflow.</p>", "messageDescription": "Description for message", "subjectReport": "Workflow delays | Your action required", "subjectGrant": "Workflow delays | Your action required", "subjectDescription": "Description for reminder notification subject", "time": "05:00", "timeDescription": "Description for time", "configuration": { "daysBefore": [ 0 ], "afterNoOfHours": [ 5760 ] }, "configurationDescription": "Description for configuration", "sql": "" }' where config_name='ACTION_DUE_REPORTS_REMINDER_SETTINGS'