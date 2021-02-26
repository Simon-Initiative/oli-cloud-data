select x.admit_code     as section,
       x.user_guid      as user_guid,
       y.resource_title,
       y.resource_id,
       y.rtype,
       y.date_available,
       y.date_submitted,
       y.date_scored,
       y.overall_score,
       y.question_id,
       y.interaction_id as interaction_id,
       y.part_id,
       y.input_value    as input_value,
       y.correct
from (select reg.user_guid, sec.admit_code
      from course.registration reg
               join course.section sec on reg.section_guid = sec.guid
      where sec.admit_code in (:admit_code)) as x
         join (select qa.id   as question_attempt_id,
                      qa.date_scored,
                      qs.question_id,
                      r.title as resource_title,
                      r.id    as resource_id,
                      r.type  as rtype,
                      r.guid,
                      itm.purpose,
                      aa.attempt_number,
                      a.user_guid,
                      a.overall_score,
                      sec.admit_code,
                      act.date_available,
                      qa.date_submitted,
                      it.interaction_id,
                      pt.part_id,
                      re.input_value,
                      ps.correct,
                      ps.errors,
                      ps.hints
               from course.section sec
                        join syllabus.syllabus syl on sec.guid = syl.section_guid
                        join syllabus.activity act on syl.guid = act.syllabus_guid
                        join content.item itm on act.item_guid = itm.guid
                        join content.resource r on act.resource_guid = r.guid
                        join assessment2.assessment_activity a on act.guid = a.activity_guid
                        join assessment2.assessment_attempt aa on a.id = aa.assessment_activity_id
                        join assessment2.question_attempt qa on aa.id = qa.assessment_attempt_id
                        join assessment2.question qs on qs.id = qa.question_id
                        join assessment2.interaction it on qs.id = it.question_id
                        join assessment2.part pt on it.question_id = pt.question_id and it.position = pt.position
                        left join assessment2.response re
                                  on qa.id = re.question_attempt_id and it.id = re.interaction_id
                        left join assessment2.PerformanceSummary ps
                                  on r.guid = ps.resourceGuid and qs.question_id = ps.questionId and
                                     pt.part_id = ps.partId and a.user_guid = ps.userGuid
               where sec.admit_code in (:admit_code)
                 and r.type in ('x-oli-assessment2')
                 and r.id = :quiz_id
)
    as y on x.user_guid = y.user_guid
order by user_guid;