package models

import org.scalatest.FunSuite

class DeploymentEnvironmentTest extends FunSuite {
  test("fromString works given prod") {
    val result = DeploymentEnvironment.fromString("prod")
    assert(result.getClass == Prod.getClass)
  }

  test("fromString works given staging") {
    val result = DeploymentEnvironment.fromString("staging")
    assert(result.getClass == Staging.getClass)
  }
}
