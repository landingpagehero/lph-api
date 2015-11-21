package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._

/**
 * A landing page document.
 */
case class LandingPage(
                        jobNumber: String,
                        name: String,
                        gitUri: String,
                        id: String = BSONObjectID.generate.stringify,
                        createdAt: DateTime = new DateTime
                        ) {
  /**
   * Serialize the landing page as JSON.
   */
  def toJson = Json.obj(
    "id" -> this.id,
    "jobNumber" -> this.jobNumber,
    "gitUri" -> this.gitUri,
    "name" -> this.name,
    "createdAt" -> this.createdAt.toString
  )
}

object LandingPage {

  implicit object LandingPageBSONReader extends BSONDocumentReader[LandingPage] {
    def read(document: BSONDocument): LandingPage = {
      LandingPage(
        document.getAs[BSONString]("jobNumber").get.value,
        document.getAs[BSONString]("name").get.value,
        document.getAs[BSONString]("gitUri").get.value,
        document.getAs[BSONObjectID]("_id").get.stringify,
        document.getAs[BSONDateTime]("createdAt").map(dt => new DateTime(dt.value)).get
      )
    }
  }

  implicit object LandingPageBSONWriter extends BSONDocumentWriter[LandingPage] {
    def write(landingPage: LandingPage): BSONDocument = {
      BSONDocument(
        "_id" -> BSONObjectID(landingPage.id),
        "jobNumber" -> BSONString(landingPage.jobNumber),
        "name" -> BSONString(landingPage.name),
        "createdAt" -> BSONDateTime(landingPage.createdAt.getMillis),
        "gitUri" -> BSONString(landingPage.gitUri)
      )
    }
  }

}
