package controllers

import models.{LandingPageAuditEvent, LandingPage}
import play.api.mvc._
import play.api.libs.json._
import javax.inject.Inject
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson._
import scala.concurrent.Future
import play.api.Logger
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._

import scala.sys.process.Process

case class LandingPageForm(jobNumber: String, name: String, gitUri: String) {
  def toLandingPage: LandingPage = LandingPage(jobNumber, name, gitUri)
}

class LandingPageManagementApi @Inject()(val reactiveMongoApi: ReactiveMongoApi)
  extends Controller
  with MongoController
  with ReactiveMongoComponents {

  def landingPagesCollection: BSONCollection = db.collection[BSONCollection]("landingPages")

  def landingPageAuditEventsCollection: BSONCollection = db.collection[BSONCollection]("landingPageAuditEvents")

  implicit val readerForLandingPages = LandingPage.LandingPageBSONReader
  implicit val writeForLandingPages = LandingPage.LandingPageBSONWriter

  implicit val readerForAuditEvents = LandingPageAuditEvent.LandingPageAuditEventBSONReader
  implicit val writeForAuditEvents = LandingPageAuditEvent.LandingPageAuditEventBSONWriter

  /**
   * Create a landing page.
   */
  def add = Action.async(parse.json) { req =>
    implicit val landingPageFormFormat = Json.format[LandingPageForm]

    Json.fromJson[LandingPageForm](req.body).fold(
      invalid => Future.successful(BadRequest("Bad landing page form")),
      valid = form => {
        val landingPage = form.toLandingPage

        // Log the creation of the landing page.
        landingPageAuditEventsCollection.insert(new LandingPageAuditEvent(
          BSONObjectID(landingPage.id),
          "Created landing page",
          s"Name: ${landingPage.name}"
        ))
        Logger.info(s"Creating landing page ${landingPage.id}, name ${landingPage.name}")

        // Clone the Git repository.
        val gitTarget = s"/home/lph/landingpages/${landingPage.jobNumber}"
        Process(s"git clone ${landingPage.gitUri} $gitTarget")
        landingPageAuditEventsCollection.insert(new LandingPageAuditEvent(
          BSONObjectID(landingPage.id),
          "Cloned repository",
          s"Cloned Git repository ${landingPage.gitUri} to $gitTarget"
        ))
        Logger.info(s"Cloned ${landingPage.gitUri} to $gitTarget")

        // Insert the landing page and wait for it to be inserted, so we can then get the new count of landing pages.
        val futures = for {
          writeResult <- landingPagesCollection.insert(landingPage)
          count <- landingPagesCollection.count()
        } yield count

        futures.map { count =>
          Created(Json.obj(
            "created" -> true,
            "message" -> s"You created landing page ${landingPage.name} (${landingPage.jobNumber}). The Git URI is '${landingPage.gitUri}'.",
            "landingPage" -> landingPage.toJson,
            "count" -> count
          ))
        }
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
   * Find one landing page.
   */
  def findOne(id: String) = Action.async {
    val bsonId = BSONObjectID(id)

    landingPagesCollection
      .find(BSONDocument("_id" -> bsonId))
      .cursor[LandingPage]()
      .collect[List](1)
      .map { landingPages =>
        val landingPage = landingPages.head
        Ok(Json.obj(
          "landingPage" -> landingPage.toJson
        ))
      }
  }

  /**
   * Find one landing page's audit log.
   */
  def findOneAuditLog(id: String) = Action.async {
    val bsonId = BSONObjectID(id)

    landingPageAuditEventsCollection
      .find(BSONDocument("landingPage" -> bsonId))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[LandingPageAuditEvent]()
      .collect[List]()
      .map { events =>
        Ok(Json.obj(
          "auditLog" -> events.map{_.toJson}
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
