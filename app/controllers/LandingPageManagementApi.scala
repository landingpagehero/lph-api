package controllers

import java.io.File

import com.github.tototoshi.csv.CSVWriter
import models._
import play.api.mvc._
import play.api.libs.json._
import repository._
import services.Git
import scala.concurrent.Future
import play.api.{Play, Logger}
import play.api.Play.current
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.json._

case class LandingPageForm(jobNumber: String, name: String, gitUri: String) {
  def toLandingPage: LandingPage = LandingPage(jobNumber, name, gitUri)
}

case class LandingPageEditForm(description: Option[String])

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
   * Edit a landing page.
   */
  def edit(id: String) = Action.async(parse.json) { req =>
    implicit val formFormat = Json.format[LandingPageEditForm]

    Json.fromJson[LandingPageEditForm](req.body).fold(
      invalid => Future.successful(BadRequest("Bad landing page form")),
      valid = form => {
        LandingPageRepository
          .findOne(id)
          .map { maybeLandingPage =>
            val landingPage = maybeLandingPage.get
            landingPage.description = form.description

            LandingPageRepository.update(landingPage)

            Logger.info(s"Editing landing page ${landingPage.id}, name ${landingPage.name}")
            LandingPageAuditEventRepository.logEvent(landingPage, "Edited landing page", s"Name: ${landingPage.name}")

            Ok(Json.obj(
              "landingPage" -> landingPage.toJson
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
          "landingPage" -> landingPage.get.toJson
        ))
      }
  }

  /**
   * Find one landing page's audit log.
   */
  def findOneAuditLog(id: String) = Action.async {
    LandingPageAuditEventRepository.getAuditLog(id).map { events =>
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
  def findOneCodeChangesLog(id: String, branch: Branch) = Action.async {
    LandingPageRepository
      .findOne(id)
      .map { landingPage =>
        val commits = Git.getCommits(landingPage.get, branch)

        Ok(Json.obj(
          "codeChanges" -> commits
        ))
      }
  }

  /**
   * Find one landing page's user events log (i.e. page views).
   */
  def findOneUserEventLog(id: String, env: DeploymentEnvironment) = Action.async {
    LandingPageUserEventRepository.getEventLog(id, env)
      .map { events =>
        Ok(Json.obj(
          "events" -> events.map(_.toJson)
        ))
      }
  }

  /**
   * Find one landing page's form submissions.
   */
  def findOneFormSubmissions(id: String, env: DeploymentEnvironment) = Action.async {
    LandingPageSubmissionRepository.getSubmissions(id, env)
      .map { submissions =>
        Ok(Json.obj(
          "submissions" -> submissions.map(_.toJson)
        ))
      }
  }

  /**
   * Find one landing page's form submissions.
   */
  def downloadOneFormSubmissionsAsCsv(id: String, env: DeploymentEnvironment) = Action.async {
    LandingPageSubmissionRepository.getSubmissions(id, env)
      .map { submissions =>
        val csvFile = new File(s"/home/lph/downloads/$id-${System.currentTimeMillis()}.csv")
        val writer = CSVWriter.open(csvFile)
        submissions.foreach { submission =>
          // @todo - how to do this?
          writer.writeRow(List(submission.id, submission.createdAt, submission.submittedData))
        }
        writer.close()

        Ok(Json.obj(
          "downloadUrl" -> s"http://${Play.configuration.getString("lph.host.downloads").get}/${csvFile.getName}"
        ))
      }
  }

  /**
   * Delete a landing page.
   */
  def delete(id: String) = Action { request =>
    LandingPageRepository.remove(id)

    Logger.info(s"Deleted landing page $id")

    Ok(Json.obj(
      "deleted" -> true
    ))
  }

  def deployToProd(id: String, branch: Branch) = Action.async { request =>
    LandingPageRepository
      .findOne(id)
      .map { maybeLandingPage =>
        Git.deploy(maybeLandingPage.get, branch, Prod)

        Ok(Json.obj(
          "deployed" -> true,
          "url" -> maybeLandingPage.get.getUrlForEnv(Prod)
        ))
      }
  }

  def deployToStaging(id: String, branch: Branch) = Action.async { request =>
    LandingPageRepository
      .findOne(id)
      .map { maybeLandingPage =>
        Git.deploy(maybeLandingPage.get, branch, Staging)

        Ok(Json.obj(
          "deployed" -> true,
          "url" -> maybeLandingPage.get.getUrlForEnv(Staging)
        ))
      }
  }
}
