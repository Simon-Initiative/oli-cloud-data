select :quiz_id as resource_guid,
       x.user_guid,
       if(y.user_guid IS NULL, 'not accessed', y.last_accessed) as last_accessed,
       ifnull(y.date_started, 'not started') as date_started,
       ifnull(y.date_completed, 'incomplete') as date_completed,
       if(y.date_completed IS NOT NULL, TIMEDIFF(y.date_completed, y.date_started), 'null') as diff,
       y.overall_score
from (select reg.user_guid, sec.admit_code
      from course.registration reg
               join course.section sec on reg.section_guid = sec.guid
      where sec.admit_code in (:admit_code)) as x
         left join (select act.guid as act_guid,
                           a.overall_score,
                           a.user_guid,
                           r.id,
                           a.last_accessed,
                           a.date_started,
                           a.date_completed
                    from course.section sec
                             join syllabus.syllabus syl on sec.guid = syl.section_guid
                             join syllabus.activity act on syl.guid = act.syllabus_guid
                             join content.resource r on act.resource_guid = r.guid
                             join assessment2.assessment_activity a on act.guid = a.activity_guid
                    where sec.admit_code in (:admit_code)
                      and r.type in ('x-oli-assessment2')
                      and r.id in (:quiz_id)
) as y on x.user_guid = y.user_guid
order by user_guid;