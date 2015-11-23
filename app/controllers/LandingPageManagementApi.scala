package controllers

import models.LandingPage
import play.api.mvc._
import play.api.libs.json._
import javax.inject.Inject
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import repository.{LandingPageRepository, LandingPageAuditEventRepository}
import services.Git
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

class LandingPageManagementApi extends Controller {

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
        LandingPageAuditEventRepository.logEvent(landingPage, "Created landing page", s"Name: ${landingPage.name}")
        Logger.info(s"Creating landing page ${landingPage.id}, name ${landingPage.name}")

        Git.cloneLandingPage(landingPage)

        // Insert the landing page and wait for it to be inserted, so we can then get the new count of landing pages.
        val futures = for {
          writeResult <- LandingPageRepository.insert(landingPage)
          count <- LandingPageRepository.count()
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
    LandingPageRepository.findAll()
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
    LandingPageRepository
      .findOne(id)
      .map { landingPage =>
        Ok(Json.obj(
          "landingPage" -> landingPage.toJson
        ))
      }
  }

  /**
   * Find one landing page's audit log.
   */
  def findOneAuditLog(id: String) = Action.async {
    LandingPageAuditEventRepository.getAuditLog((id)).map { events =>
      Ok(Json.obj(
        "auditLog" -> events.map {
          _.toJson
        }
      ))
    }
  }

  /**
   * Find one landing page's code changes log (i.e. the Git log).
   */
  def findOneCodeChangesLog(id: String, branch: String) = Action.async {
    LandingPageRepository
      .findOne(id)
      .map { landingPage =>
        val commits = Git.getCommits(landingPage, branch)

        Ok(Json.obj(
          "codeChanges" -> commits
        ))
      }
  }

  /**
   * Delete a landing page.
   */
  def delete(id: String) = Action { request =>
    LandingPageRepository.remove(id)

    Logger.info(s"Deleted landing page $id");

    Ok(Json.obj(
      "deleted" -> true
    ))
  }
}
