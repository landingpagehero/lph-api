package models

case class DeploymentEnvironment(envName: String) {
  val path = s"/home/lph/landingpages/$envName"
}

object DeploymentEnvironment {
  implicit def fromString(envName: String): DeploymentEnvironment = envName match {
    case "prod" => Prod
    case "staging" => Staging
    case _ => throw new Exception
  }
}

object Prod extends DeploymentEnvironment("prod")

object Staging extends DeploymentEnvironment("staging")
