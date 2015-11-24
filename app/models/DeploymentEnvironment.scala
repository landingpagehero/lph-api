package models

class DeploymentEnvironment(env: String) {
  val envName = env
  val path = s"/home/lph/landingpages/$env"
}

object Prod extends DeploymentEnvironment("prod")

object Staging extends DeploymentEnvironment("staging")
