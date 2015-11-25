package models

import org.joda.time.DateTime
import play.api.Play
import play.api.libs.json.{JsNull, Json}
import reactivemongo.bson._
import play.api.Play.current
import play.modules.reactivemongo.json.BSONFormats._

/**
 * A landing page document.
 */
case class LandingPage(
                        jobNumber: String,
                        name: String,
                        gitUri: String,
                        id: String = BSONObjectID.generate.stringify,
                        createdAt: DateTime = new DateTime,
                        var lastFetchedRepoAt: Option[DateTime] = None
                        ) {
  /**
   * Serialize the landing page as JSON.
   */
  def toJson = Json.obj(
    "id" -> id,
    "jobNumber" -> jobNumber,
    "gitUri" -> gitUri,
    "name" -> name,
    "createdAt" -> createdAt.toString,
    "lastFetchedRepoAt" -> {
      if (lastFetchedRepoAt.isDefined) lastFetchedRepoAt.get.toString
      else JsNull
    },
    "prodUrl" -> getProdUrl,
    "stagingUrl" -> getStagingUrl
  )

  implicit def toBsonId: BSONObjectID = BSONObjectID(id)

  def getProdUrl = s"http://${jobNumber.toLowerCase}-prod.${Play.configuration.getString("lph.host.sites").get}/"

  def getStagingUrl = s"http://${jobNumber.toLowerCase}-staging.${Play.configuration.getString("lph.host.sites").get}/"

  def getUrlForEnv(env: DeploymentEnvironment) = {
    if (env.getClass == Prod.getClass) getProdUrl
    else if (env.getClass == Staging.getClass) getStagingUrl
    else throw new Exception("Unknown deployment environment: " + env)
  }
}

object LandingPage {

  implicit object LandingPageBSONReader extends BSONDocumentReader[LandingPage] {
    def read(document: BSONDocument): LandingPage = {
      LandingPage(
        document.getAs[BSONString]("jobNumber").get.value,
        document.getAs[BSONString]("name").get.value,
        document.getAs[BSONString]("gitUri").get.value,
        document.getAs[BSONObjectID]("_id").get.stringify,
        document.getAs[BSONDateTime]("createdAt").map(dt => new DateTime(dt.value)).get,
        document.getAs[BSONDateTime]("lastFetchedRepoAt").map(dt => new DateTime(dt.value))
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
        "gitUri" -> BSONString(landingPage.gitUri),
        "lastFetchedRepoAt" -> Option[BSONDateTime](
          if (landingPage.lastFetchedRepoAt.isDefined) BSONDateTime(landingPage.lastFetchedRepoAt.get.getMillis)
          else null
        )
      )
    }
  }

}
