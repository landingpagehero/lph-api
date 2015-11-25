package controllers

import java.io.File
import java.nio.charset.StandardCharsets
import models.{LandingPageSubmission, DeploymentEnvironment, LandingPageUserEvent}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.BSONFormats
import play.modules.reactivemongo.json.BSONFormats.{BSONArrayFormat, BSONDocumentFormat}
import reactivemongo.bson._
import repository.{LandingPageSubmissionRepository, LandingPageUserEventRepository, LandingPageRepository}
import org.apache.commons.codec.binary

import scala.io.Source

class ServeSites extends Controller {

  def serveFile(path: String): Action[AnyContent] = Action.async { req: Request[AnyContent] =>
    val pathToServe = if (path == "") "index.html" else path

    val jobNumber = getJobNumberFromRequest(req)

    val env = getEnvFromRequest(req)

    LandingPageRepository.findOneByJobNumberCaseInsensitive(jobNumber)
      .map { maybeLandingPage =>
        if (maybeLandingPage.isEmpty) NotFound
        else {
          // The job number in the host might be a different case from the one on the file system.
          // Ensure we use the one in the file system, not in the host.
          val fileOnFileSystem = new File(s"${env.path}/${maybeLandingPage.get.jobNumber}/$pathToServe")

          if (!fileOnFileSystem.exists) NotFound
          else {
            val mimeType = getMimeTypeForFile(fileOnFileSystem)

            if (shouldLogMimeType(mimeType)) {
              val landingPageUserEvent = new LandingPageUserEvent(
                BSONObjectID(maybeLandingPage.get.id),
                "Viewed " + pathToServe,
                env,
                req.remoteAddress
              )
              LandingPageUserEventRepository.insert(landingPageUserEvent)
            }

            if (mimeType == "text/html" && req.cookies.nonEmpty) {
              val source = Source.fromFile(fileOnFileSystem)
              var content = try source.mkString finally source.close()

              req.cookies.foreach { cookie => {
                content = content.replaceAllLiterally(s"{{{ ${cookie.name} }}}", fromBase64(cookie.value))
                content = content.replaceAllLiterally(s"{{{${cookie.name}}}}", fromBase64(cookie.value))
              }}

              Ok(content).withHeaders("Content-Type" -> mimeType)
            }
            else Ok.sendFile(fileOnFileSystem, inline = true).withHeaders(
              "Content-Type" -> mimeType
            )
          }
        }
      }
  }

  def recordFormSubmission(path: String) = Action.async { req =>
    val jobNumber = getJobNumberFromRequest(req)
    LandingPageRepository.findOneByJobNumberCaseInsensitive(jobNumber).map { maybeLandingPage =>
      if (maybeLandingPage.isEmpty) NotFound
      else {
        val landingPage = maybeLandingPage.get

        val submittedDataToSave =
          if (req.body.asFormUrlEncoded.isEmpty) Option.empty
          else Option(req.body.asFormUrlEncoded.get)
        LandingPageSubmissionRepository.insertSubmission(landingPage, submittedDataToSave, getEnvFromRequest(req))

        LandingPageUserEventRepository.insert(new LandingPageUserEvent(
          landingPage.toBsonId,
          "Form submitted",
          getEnvFromRequest(req),
          req.remoteAddress
        ))

        if (req.body.asFormUrlEncoded.isEmpty) SeeOther(url = "/" + path)
        else {
          var response = SeeOther("/" + path).withCookies()
          req.body.asFormUrlEncoded.get.foreach({ case (key, values) =>
            // Cookies values can't have many characters in them, so base 64 encode the value so we can actually
            // set the cookies regardless of what characters are submitted.
            response = response.withCookies(new Cookie(key, toBase64(values.head)))
          })
          response
        }
      }
    }
  }

  private def getMimeTypeForFile(file: File): String = file.getName drop file.getName.lastIndexOf('.') match {
    case ".html" => "text/html"
    case ".css" => "text/css"
    case ".js" => "application/javascript"
    case _ => "application/octet-stream"
  }

  private def shouldLogMimeType(mimeType: String): Boolean = mimeType match {
    case "text/html" => true
    case "application/octet-stream" => true
    case _ => false
  }

  private def toBase64(value: String): String = binary.Base64.encodeBase64String(value.getBytes(StandardCharsets.UTF_8))

  private def fromBase64(base64: String): String = binary.Base64.decodeBase64(base64).map(_.toChar).mkString

  private def getJobNumberFromRequest(request: Request[_]): String = {
    // Host is like "[job-number]-[env].lph.dev" - extract the job number.
    request.host.split('-').apply(0)
  }

  private def getEnvFromRequest(request: Request[_]): DeploymentEnvironment = {
    // Host is like "[job-number]-[env].lph.dev" - extract the env.
    request.host.split('-').apply(1).split('.').apply(0)
  }
}
