package controllers

import java.io.File

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import repository.LandingPageRepository

class ServeSites extends Controller {

  def serveFile(path: String): Action[AnyContent] = Action.async { req: Request[AnyContent] =>
    val pathToServe = if (path == "") "index.html" else path

    // Host is like "[job-number]-[env].lph.dev" - extract the job number.
    val jobNumber = req.host.split('-').apply(0)

    // Host is like "[job-number]-[env].lph.dev" - extract the env.
    val env = req.host.split('-').apply(1).split('.').apply(0)

    LandingPageRepository.findOneByJobNumberCaseInsensitive(jobNumber)
      .map { maybeLandingPage =>
        if (maybeLandingPage.isEmpty) NotFound
        else {
          // The job number in the host might be a different case from the one on the file system.
          // Ensure we use the one in the file system, not in the host.
          val fileOnFileSystem = new File(s"/home/lph/landingpages/$env/${maybeLandingPage.get.jobNumber}/$pathToServe")

          if (!fileOnFileSystem.exists) NotFound
          else {
            Ok.sendFile(fileOnFileSystem, inline = true).withHeaders(
              "Content-Type" -> getMimeTypeForFile(fileOnFileSystem)
            )
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

}
