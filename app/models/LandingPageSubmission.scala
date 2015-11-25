package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson._

/**
 * A landing page audit event document.
 */
case class LandingPageSubmission(
                                  landingPage: BSONObjectID,
                                  submittedData: BSONValue,
                                  environment: DeploymentEnvironment,
                                  id: String = BSONObjectID.generate.stringify,
                                  createdAt: DateTime = new DateTime
                                  ) {
  /**
   * Serialize the event as JSON.
   */
  def toJson = Json.obj(
    "id" -> id,
    "submittedData" -> BSONFormats.toJSON(submittedData),
    "createdAt" -> createdAt.toString
  )
}

object LandingPageSubmission {

  implicit object LandingPageSubmissionBSONReader extends BSONDocumentReader[LandingPageSubmission] {
    def read(document: BSONDocument): LandingPageSubmission = {
      LandingPageSubmission(
        document.getAs[BSONObjectID]("landingPage").get,
        document.getAs[BSONValue]("submittedData").get,
        DeploymentEnvironment.fromString(document.getAs[BSONString]("environment").get.value),
        document.getAs[BSONObjectID]("_id").get.stringify,
        document.getAs[BSONDateTime]("createdAt").map(dt => new DateTime(dt.value)).get
      )
    }
  }

  implicit object LandingPageSubmissionBSONWriter extends BSONDocumentWriter[LandingPageSubmission] {
    def write(event: LandingPageSubmission): BSONDocument = {
      BSONDocument(
        "landingPage" -> event.landingPage,
        "submittedData" -> event.submittedData,
        "environment" -> BSONString(event.environment.toString),
        "createdAt" -> BSONDateTime(event.createdAt.getMillis)
      )
    }
  }

}
