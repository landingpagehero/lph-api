package repository

import models.{LandingPage, LandingPageAuditEvent}
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import scala.concurrent.Future

object LandingPageAuditEventRepository {
  private lazy val reactiveMongoApi = Play.current.injector.instanceOf[ReactiveMongoApi]

  private def collection: BSONCollection = reactiveMongoApi.db.collection[BSONCollection]("landingPageAuditEvents")

  private implicit val reader = LandingPageAuditEvent.LandingPageAuditEventBSONReader
  private implicit val writer = LandingPageAuditEvent.LandingPageAuditEventBSONWriter

  def getAuditLog(id: String): Future[List[LandingPageAuditEvent]] = {
    collection
      .find(BSONDocument("landingPage" -> BSONObjectID(id)))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[LandingPageAuditEvent]()
      .collect[List]()
  }

  def logEvent(landingPage: LandingPage, eventType: String, message: String) = {
    val event = new LandingPageAuditEvent(BSONObjectID(landingPage.id), eventType, message)
    collection.insert(event)
  }

}
