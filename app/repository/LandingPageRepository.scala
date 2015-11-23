package repository

import models.LandingPage
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import scala.concurrent.Future

object LandingPageRepository {
  private lazy val reactiveMongoApi = Play.current.injector.instanceOf[ReactiveMongoApi]

  private def collection: BSONCollection = reactiveMongoApi.db.collection[BSONCollection]("landingPages")

  private implicit val reader = LandingPage.LandingPageBSONReader
  private implicit val writer = LandingPage.LandingPageBSONWriter

  def insert(landingPage: LandingPage) = {
    collection.insert(landingPage)
  }

  def count() = {
    collection.count()
  }

  def findAll(): Future[List[LandingPage]] = {
    collection
      .find(BSONDocument())
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[LandingPage]()
      .collect[List]()
  }

  def findOne(id: String): Future[Option[LandingPage]] = {
    val bsonId = BSONObjectID(id)

    collection
      .find(BSONDocument("_id" -> bsonId))
      .one[LandingPage]
  }

  def remove(id: String) = {
    collection.remove(BSONDocument(
      "_id" -> BSONObjectID(id)
    ))
  }

  def update(landingPage: LandingPage) = {
    collection.update(
      BSONDocument("_id" -> BSONObjectID(landingPage.id)),
      landingPage
    )
  }

}
