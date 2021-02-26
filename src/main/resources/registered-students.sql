select r.user_guid
from course.registration r
         join course.section s on r.section_guid = s.guid
where s.admit_code = :admit_code
  and r.role = 'student';