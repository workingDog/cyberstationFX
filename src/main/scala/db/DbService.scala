package db

import com.kodekutters.stix.Bundle
import cyber.CyberBundle
import reactivemongo.api.commands.MultiBulkWriteResult
import scala.concurrent.Future


trait DbService {
  def init(): Unit

  def close(): Unit

  def saveServerBundle(bundle: Bundle, colPath: String): Unit

  def saveLocalBundles(cyberList: List[CyberBundle]): Future[(MultiBulkWriteResult, MultiBulkWriteResult)]

  def loadLocalBundles(): Future[List[CyberBundle]]

  def dropLocalBundles(): Unit
}


object DbService extends DbService {

  val mongoDB = MongoDbService

  val dbUri = mongoDB.dbUri

  def isConnected() = mongoDB.isConnected

  def init(): Unit = {
    mongoDB.init()
  }

  def close(): Unit = {
    mongoDB.close()
  }

  def saveServerBundle(bundle: Bundle, colPath: String): Unit = {
    mongoDB.saveServerBundle(bundle, colPath)
  }

  def saveLocalBundles(cyberList: List[CyberBundle]): Future[(MultiBulkWriteResult, MultiBulkWriteResult)] = {
    mongoDB.saveLocalBundles(cyberList)
  }

  def loadLocalBundles(): Future[List[CyberBundle]] = {
    mongoDB.loadLocalBundles()
  }

  def dropLocalBundles(): Unit = {
    mongoDB.dropLocalBundles()
  }
}
