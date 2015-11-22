package services

import models.LandingPage
import play.api.Logger
import repository.LandingPageAuditEventRepository
import scala.sys.process.Process

object Git {
  val clonePath = "/home/lph/landingpages/clones";

  /**
   * Clone a landing page's Git repository to the server.
   */
  def cloneLandingPage(landingPage: LandingPage) = {
    val gitTarget = s"$clonePath/${landingPage.jobNumber}"
    Process(s"git clone ${landingPage.gitUri} $gitTarget").lineStream.foreach(Logger.info(_))
    LandingPageAuditEventRepository.logEvent(landingPage, "Cloned repository", s"Cloned Git repository ${landingPage.gitUri} to $gitTarget")
    Logger.info(s"Cloned ${landingPage.gitUri} to $gitTarget")
  }
}
