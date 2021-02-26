select distinct s.user_sess as login_session,
                s.user_id,
                a.source,
                a.action,
                a.server_receipt_time,
                a.info,
                su.action   as action_detail,
                su.info_type,
                su.info     as info_detail
from log.log_sess s
         join log.log_act a on s.user_sess = a.sess_ref
         left join log.log_supplement su on a.guid = su.action_guid
where s.user_id in (:students)
  and a.server_receipt_time between :starting
      and :ending;