package services

import org.joda.time.DateTime

import scala.sys.process._
import models._
import play.api.Logger
import repository.{LandingPageRepository, LandingPageAuditEventRepository}

object Git {
  val clonePath = "/home/lph/landingpages/clones"

  /**
   * Clone a landing page's Git repository to the server.
   *
   * @return The clone result from the command line, or an error message.
   */
  def cloneLandingPage(landingPage: LandingPage): String = {
    val gitTarget = getLocalClonePath(landingPage)

    Logger.info(s"Running: git clone ${landingPage.gitUri} $gitTarget")
    val cloneResult = try {
      s"git clone ${landingPage.gitUri} $gitTarget" !!
    } catch {
      case e: RuntimeException => return s"Could not clone ${landingPage.gitUri}"
    }

    LandingPageAuditEventRepository.logEvent(landingPage, "Cloned repository", s"Cloned Git repository ${landingPage.gitUri} to $gitTarget:\n$cloneResult")
    Logger.info(s"Cloned ${landingPage.gitUri} to $gitTarget: $cloneResult")

    deploy(landingPage, "master", Prod)
    deploy(landingPage, "master", Staging)

    cloneResult
  }

  /**
   * Get all commits in the given branch.
   *
   * @return The commit log, or an error message.
   */
  def getCommits(landingPage: LandingPage, branch: String): String = {
    val gitTarget = getLocalClonePath(landingPage)

    val fetchResult = try {
      s"git -C $gitTarget fetch" !!
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
      s"git -C $gitTarget log --no-merges origin/$branch" !!
    } catch {
      case e: RuntimeException => {
        LandingPageAuditEventRepository.logEvent(landingPage, s"Failed to get log from Git branch", s"Branch: $branch")
        return s"Branch does not exist: $branch"
      }
    }

    logResult
  }

  /**
   * Deploy the landing page's given branch to the given environment.
   */
  def deploy(landingPage: LandingPage, branch: String, targetEnv: DeploymentEnvironment) = {
    val deployTarget = s"${targetEnv.path}/${landingPage.jobNumber}"
    val from = getLocalClonePath(landingPage)

    s"rm -fr $deployTarget" !!;
    s"cp -R $from $deployTarget" !!;
    s"git -C $deployTarget fetch origin" !!;
    s"git -C $deployTarget reset origin/$branch --hard" !!

    LandingPageAuditEventRepository.logEvent(landingPage, s"Deployed to ${targetEnv.envName}", landingPage.getUrlForEnv(targetEnv))
    Logger.info(s"Deployed landing page ${landingPage.id} to ${targetEnv.envName}. URL is ${landingPage.getUrlForEnv(targetEnv)}")
  }

  private def getLocalClonePath(landingPage: LandingPage): String = {
    s"$clonePath/${landingPage.jobNumber}"
  }
}
