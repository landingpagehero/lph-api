package repository

import models.{DeploymentEnvironment, LandingPage, LandingPageSubmission}
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, JsValue}
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocument, BSONObjectID}
import scala.concurrent.Future

object LandingPageSubmissionRepository {
  private lazy val reactiveMongoApi = Play.current.injector.instanceOf[ReactiveMongoApi]

  private def collection: BSONCollection = reactiveMongoApi.db.collection[BSONCollection]("landingPageSubmissions")

  private implicit val reader = LandingPageSubmission.LandingPageSubmissionBSONReader
  private implicit val writer = LandingPageSubmission.LandingPageSubmissionBSONWriter

  def getSubmissions(landingPageId: String): Future[List[LandingPageSubmission]] = {
    collection
      .find(BSONDocument("landingPage" -> BSONObjectID(landingPageId)))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[LandingPageSubmission]()
      .collect[List]()
  }

  def insertSubmission(
                        landingPage: LandingPage,
                        submittedData: Option[Map[String, Seq[String]]],
                        env: DeploymentEnvironment
                        ) = {
    val submittedDataAsBson = BSONFormats.toBSON(Json.toJson(submittedData)).get
    val submission = new LandingPageSubmission(landingPage.toBsonId, submittedDataAsBson, env)
    collection insert submission
  }

}
