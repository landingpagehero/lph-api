package repository

import models.{DeploymentEnvironment, LandingPageUserEvent}
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONObjectID, BSONDocument}

import scala.concurrent.Future

object LandingPageUserEventRepository {
  private lazy val reactiveMongoApi = Play.current.injector.instanceOf[ReactiveMongoApi]

  private def collection: BSONCollection = reactiveMongoApi.db.collection[BSONCollection]("landingPageUserEvents")

  private implicit val reader = LandingPageUserEvent.LandingPageUserEventBSONReader
  private implicit val writer = LandingPageUserEvent.LandingPageUserEventBSONWriter

  def insert(event: LandingPageUserEvent) = {
    collection.insert(event)
  }

  def getEventLog(id: String, env: DeploymentEnvironment): Future[List[LandingPageUserEvent]] = {
    collection
      .find(BSONDocument("landingPage" -> BSONObjectID(id), "environment" -> env.toString))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[LandingPageUserEvent]()
      .collect[List]()
  }

}
