package db

import play.api.libs.json.Json

/**
  *
  * @param user_id
  * @param bundle_id
  * @param stix_id
  * @param sent
  * @param col_id
  */
case class UserLog(user_id: String, bundle_id: String,
                   stix_id: String, col_id: String, sent: String)

object UserLog {
  implicit val fmt = Json.format[UserLog]
}
