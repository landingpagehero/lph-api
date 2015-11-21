package controllers

import models.LandingPage
import play.api.mvc._
import play.api.libs.json._
import javax.inject.Inject
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import scala.concurrent.Future
import play.api.Logger
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._

case class LandingPageForm(jobNumber: String, name: String, gitUri: String) {
  def toLandingPage: LandingPage = LandingPage(jobNumber, name, gitUri)
}

class LandingPageManagementApi @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller
  with MongoController
  with ReactiveMongoComponents {

  def landingPagesCollection: BSONCollection = db.collection[BSONCollection]("landingPages")

  implicit val reader = LandingPage.LandingPageBSONReader
  implicit val write = LandingPage.LandingPageBSONWriter

  /**
   * Create a landing page.
   */
  def add = Action.async(parse.json) { req =>
    implicit val landingPageFormFormat = Json.format[LandingPageForm]

    Json.fromJson[LandingPageForm](req.body).fold(
      invalid => Future.successful(BadRequest("Bad landing page form")),
      valid = form => {
        val landingPage = form.toLandingPage

        Logger.info(s"Created landing page ${landingPage.id}");

        landingPagesCollection
          .insert(landingPage)
          .map(_ => Created(Json.obj(
            "created" -> true,
            "message" -> s"You created landing page ${landingPage.name} (${landingPage.jobNumber}). The Git URI is '${landingPage.gitUri}'.",
            "landingPage" -> landingPage.toJson
          )))
      }
    )
  }

  /**
   * Find all landing pages, sorted with newest first.
   */
  def findAll = Action.async {
    landingPagesCollection
      .find(BSONDocument())
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[LandingPage]()
      .collect[List]()
      .map { landingPages =>
        Ok(Json.obj(
          "landingPages" -> landingPages.map(lp => lp.toJson),
          "count" -> landingPages.length
        ))
      }
  }

  /**
   * Delete a landing page.
   */
  def delete(id: String) = Action { request =>
    landingPagesCollection.remove(BSONDocument({
      "_id" -> BSONObjectID(id)
    }))

    Logger.info(s"Deleted landing page $id");

    Ok(Json.obj(
      "deleted" -> true
    ))
  }
}
