package controllers

import models.Developer
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

case class DeveloperForm(githubUsername: String, realName: String, email: String) {
  def toDeveloper: Developer = Developer(githubUsername , realName, email)
}

class DeveloperManagementApi @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller
  with MongoController
  with ReactiveMongoComponents {

  def developersCollection: BSONCollection = db.collection[BSONCollection]("developers")

  implicit val reader = Developer.DeveloperBSONReader
  implicit val write = Developer.DeveloperBSONWriter

  /**
   * Create a developer.
   */
  def add = Action.async(parse.json) { req =>
    implicit val developerFormFormat = Json.format[DeveloperForm]

    Json.fromJson[DeveloperForm](req.body).fold(
      invalid => Future.successful(BadRequest("Bad developer form")),
      valid = form => {
        val developer = form.toDeveloper

        Logger.info(s"Created developer ${developer.id}");

        developersCollection
          .insert(developer)
          .map(_ => Created(Json.obj(
            "created" -> true,
            "developer" -> developer.toJson
          )))
      }
    )
  }

  /**
   * Find all developers, sorted with newest first.
   */
  def findAll = Action.async {
    developersCollection
      .find(BSONDocument())
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Developer]()
      .collect[List]()
      .map { developers =>
        Ok(Json.obj(
          "developers" -> developers.map(developer => developer.toJson),
          "count" -> developers.length
        ))
      }
  }

  /**
   * Delete a developer.
   */
  def delete(id: String) = Action { request =>
    developersCollection.remove(BSONDocument({
      "_id" -> BSONObjectID(id)
    }))

    Logger.info(s"Deleted developer $id");

    Ok(Json.obj(
      "deleted" -> true
    ))
  }
}
