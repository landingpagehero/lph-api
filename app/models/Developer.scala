package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._

/**
 * A developer document.
 */
case class Developer(
                      githubUsername: String,
                      realName: String,
                      email: String,
                      id: String = BSONObjectID.generate.stringify,
                      createdAt: DateTime = new DateTime
                      ) {
  /**
   * Serialize the developer as JSON.
   */
  def toJson = Json.obj(
    "id" -> this.id,
    "githubUsername" -> this.githubUsername,
    "realName" -> this.realName,
    "email" -> this.email,
    "createdAt" -> this.createdAt.toString
  )
}

object Developer {

  implicit object DeveloperBSONReader extends BSONDocumentReader[Developer] {
    def read(document: BSONDocument): Developer = {
      Developer(
        document.getAs[BSONString]("githubUsername").get.value,
        document.getAs[BSONString]("realName").get.value,
        document.getAs[BSONString]("email").get.value,
        document.getAs[BSONObjectID]("_id").get.stringify,
        document.getAs[BSONDateTime]("createdAt").map(dt => new DateTime(dt.value)).get
      )
    }
  }

  implicit object DeveloperBSONWriter extends BSONDocumentWriter[Developer] {
    def write(landingPage: Developer): BSONDocument = {
      BSONDocument(
        "_id" -> BSONObjectID(landingPage.id),
        "createdAt" -> BSONDateTime(landingPage.createdAt.getMillis),
        "githubUsername" -> BSONString(landingPage.githubUsername),
        "email" -> BSONString(landingPage.email),
        "realName" -> BSONString(landingPage.realName)
      )
    }
  }

}
