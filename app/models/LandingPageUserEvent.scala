package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._

/**
 * A landing page user event document.
 */
case class LandingPageUserEvent(
                                 landingPage: BSONObjectID,
                                 eventType: String,
                                 environment: DeploymentEnvironment,
                                 ipAddress: String,
                                 id: String = BSONObjectID.generate.stringify,
                                 createdAt: DateTime = new DateTime
                                 ) {
  /**
   * Serialize the event as JSON.
   */
  def toJson = Json.obj(
    "id" -> id,
    "eventType" -> eventType,
    "ipAddress" -> ipAddress,
    "createdAt" -> createdAt.toString
  )
}

object LandingPageUserEvent {

  implicit object LandingPageUserEventBSONReader extends BSONDocumentReader[LandingPageUserEvent] {
    def read(document: BSONDocument): LandingPageUserEvent = {
      LandingPageUserEvent(
        document.getAs[BSONObjectID]("landingPage").get,
        document.getAs[BSONString]("eventType").get.value,
        DeploymentEnvironment.fromString(document.getAs[BSONString]("environment").get.value),
        document.getAs[BSONString]("ipAddress").get.value,
        document.getAs[BSONObjectID]("_id").get.stringify,
        document.getAs[BSONDateTime]("createdAt").map(dt => new DateTime(dt.value)).get
      )
    }
  }

  implicit object LandingPageUserEventBSONWriter extends BSONDocumentWriter[LandingPageUserEvent] {
    def write(event: LandingPageUserEvent): BSONDocument = {
      BSONDocument(
        "landingPage" -> event.landingPage,
        "eventType" -> BSONString(event.eventType),
        "environment" -> BSONString(event.environment.toString),
        "ipAddress" -> BSONString(event.ipAddress),
        "createdAt" -> BSONDateTime(event.createdAt.getMillis)
      )
    }
  }

}
