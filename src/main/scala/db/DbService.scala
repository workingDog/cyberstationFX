package db

import com.kodekutters.stix.Bundle
import cyber.CyberBundle
import db.mongo.MongoLocalService
import reactivemongo.api.commands.MultiBulkWriteResult

import scala.concurrent.Future


trait DbService {
  def init(): Unit

  def isConnected(): Boolean

  def close(): Unit

  def saveServerBundle(bundle: Bundle, colPath: String): Unit

  def saveLocalBundles(cyberList: List[CyberBundle]): Future[(MultiBulkWriteResult, MultiBulkWriteResult)]

  def loadLocalBundles(): Future[List[CyberBundle]]

  def dropLocalBundles(): Unit

  def getUri(): String

}


object DbService extends DbService {

  val mongoDB = MongoLocalService

  def getUri(): String = mongoDB.dbUri

  def isConnected(): Boolean = mongoDB.isConnected()

  def init(): Unit = mongoDB.init()

  def close(): Unit = mongoDB.close()

  def saveServerBundle(bundle: Bundle, colPath: String): Unit = mongoDB.saveServerBundle(bundle, colPath)

  def saveLocalBundles(cyberList: List[CyberBundle]): Future[(MultiBulkWriteResult, MultiBulkWriteResult)] = mongoDB.saveLocalBundles(cyberList)

  def loadLocalBundles(): Future[List[CyberBundle]] = mongoDB.loadLocalBundles()

  def dropLocalBundles(): Unit = mongoDB.dropLocalBundles()

}
