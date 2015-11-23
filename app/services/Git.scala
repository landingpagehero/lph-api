package services

import org.joda.time.DateTime

import scala.sys.process._
import models.LandingPage
import play.api.Logger
import repository.{LandingPageRepository, LandingPageAuditEventRepository}
import java.lang.RuntimeException

object Git {
  val clonePath = "/home/lph/landingpages/clones";

  /**
   * Clone a landing page's Git repository to the server.
   *
   * @return The clone result from the command line, or an error message.
   */
  def cloneLandingPage(landingPage: LandingPage): String = {
    val gitTarget = getLocalClonePath(landingPage)

    Logger.info(s"Running: git clone ${landingPage.gitUri} $gitTarget")
    val cloneResult = try {
      s"git clone ${landingPage.gitUri} $gitTarget" !!;
    } catch {
      case e: RuntimeException => return s"Could not clone ${landingPage.gitUri}"
    }

    LandingPageAuditEventRepository.logEvent(landingPage, "Cloned repository", s"Cloned Git repository ${landingPage.gitUri} to $gitTarget:\n$cloneResult")
    Logger.info(s"Cloned ${landingPage.gitUri} to $gitTarget: $cloneResult")

    cloneResult
  }

  /**
   * Get all commits in the given branch.
   *
   * @param landingPage
   * @return The commit log, or an error message.
   */
  def getCommits(landingPage: LandingPage, branch: String): String = {
    val gitTarget = getLocalClonePath(landingPage)

    val fetchResult = try {
      s"git -C $gitTarget fetch" !!;
    } catch {
      case e: RuntimeException => {
        LandingPageAuditEventRepository.logEvent(landingPage, "Failed to fetch repository", e.getMessage)
        return s"Failed to fetch latest code changes from ${landingPage.gitUri}"
      }
    }

    // Successfully fetched the repository - record that it happened.
    landingPage.lastFetchedRepoAt = Option[DateTime](new DateTime())
    LandingPageRepository.update(landingPage)
    Logger.info(s"Updated ${landingPage.name} in $gitTarget: $fetchResult")
    LandingPageAuditEventRepository.logEvent(landingPage, "Fetched repository", fetchResult)

    val logResult = try {
      s"git -C $gitTarget log --no-merges $branch" !!;
    } catch {
      case e: RuntimeException => {
        LandingPageAuditEventRepository.logEvent(landingPage, s"Failed to get log from Git branch", s"Branch: $branch")
        return s"Branch does not exist: $branch"
      }
    }

    logResult
  }

  private def getLocalClonePath(landingPage: LandingPage): String = {
    s"$clonePath/${landingPage.jobNumber}"
  }
}
