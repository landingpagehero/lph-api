package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._

/**
 * A landing page audit event document.
 */
case class LandingPageAuditEvent(
                                  landingPage: BSONObjectID,
                                  eventType: String,
                                  message: String,
                                  id: String = BSONObjectID.generate.stringify,
                                  createdAt: DateTime = new DateTime
                                  ) {
  /**
   * Serialize the event as JSON.
   */
  def toJson = Json.obj(
    "id" -> this.id,
    "eventType" -> this.eventType,
    "message" -> this.message,
    "createdAt" -> this.createdAt.toString
  )
}

object LandingPageAuditEvent {

  implicit object LandingPageAuditEventBSONReader extends BSONDocumentReader[LandingPageAuditEvent] {
    def read(document: BSONDocument): LandingPageAuditEvent = {
      LandingPageAuditEvent(
        document.getAs[BSONObjectID]("landingPage").get,
        document.getAs[BSONString]("eventType").get.value,
        document.getAs[BSONString]("message").get.value,
        document.getAs[BSONObjectID]("_id").get.stringify,
        document.getAs[BSONDateTime]("createdAt").map(dt => new DateTime(dt.value)).get
      )
    }
  }

  implicit object LandingPageAuditEventBSONWriter extends BSONDocumentWriter[LandingPageAuditEvent] {
    def write(event: LandingPageAuditEvent): BSONDocument = {
      BSONDocument(
        "landingPage" -> event.landingPage,
        "eventType" -> BSONString(event.eventType),
        "message" -> BSONString(event.message),
        "createdAt" -> BSONDateTime(event.createdAt.getMillis)
      )
    }
  }

}
