package repository

import models.Developer
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONObjectID, BSONDocument}

import scala.concurrent.Future

object DeveloperRepository {
  private lazy val reactiveMongoApi = Play.current.injector.instanceOf[ReactiveMongoApi]

  private def collection: BSONCollection = reactiveMongoApi.db.collection[BSONCollection]("developers")

  implicit val reader = Developer.DeveloperBSONReader
  implicit val write = Developer.DeveloperBSONWriter

  def insert(developer: Developer) = {
    collection.insert(developer)
  }

  def count() = {
    collection.count()
  }

  def findAll(): Future[List[Developer]] = {
    collection
      .find(BSONDocument())
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Developer]()
      .collect[List]()
  }

  def findOne(id: String): Future[Developer] = {
    val bsonId = BSONObjectID(id)

    collection
      .find(BSONDocument("_id" -> bsonId))
      .cursor[Developer]()
      .collect[List](1)
      .map(developers => developers.head)
  }

  def remove(id: String) = {
    collection.remove(BSONDocument({
      "_id" -> BSONObjectID(id)
    }))
  }

}
