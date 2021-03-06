package models

case class DeploymentEnvironment(private val envName: String) {
  val path = s"/home/lph/landingpages/$envName"

  override def toString = envName
}

object DeploymentEnvironment {
  implicit def fromString(envName: String): DeploymentEnvironment = envName match {
    case "prod" => Prod
    case "staging" => Staging
    case _ => play.Logger.error("Unknown env: " + envName); throw new Exception
  }
}

object Prod extends DeploymentEnvironment("prod")

object Staging extends DeploymentEnvironment("staging")
