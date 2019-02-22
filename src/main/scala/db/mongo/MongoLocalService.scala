package db.mongo

import com.kodekutters.stix._
import cyber.{BundleInfo, CyberBundle}
import db.{DbService, UserLog}
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.api.commands.MultiBulkWriteResult
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.{implicitConversions, postfixOps}


/**
  * the MongoDb Service support for storing local bundles data to a mongodb
  */
object MongoLocalService extends MongoBase with DbService {

  private var bundlesCol = "bundles"
  private var bundlesInf = "bundlesInfo"
  var userLogCol = "userLog"

  override def readConfig(): Unit = {
    try {
      timeout = config.getInt("mongodb.timeout")
      dbUri = config.getString("mongodb.uri")
      bundlesCol = config.getString("mongo.collection.bundles")
      bundlesInf = config.getString("mongo.collection.bundlesInfo")
      userLogCol = config.getString("mongo.collection.userLog")
    } catch {
      case e: Throwable => println("---> config error: " + e)
    }
  }

  def bundlesF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesCol))

  def bundlesInfoF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesInf))

  def userLogF: Future[JSONCollection] = database.map(_.collection[JSONCollection](userLogCol))

  def saveServerBundle(bundle: Bundle, colPath: String): Unit = {
    if (isConnected()) {
      // save the bundle of stix
      saveBundleAsStixs(bundle)
      // save the log to the db
      saveUserLog(bundle, colPath)
    }
  }

  def saveUserLog(bundle: Bundle, colPath: String): Unit = {
    for (stix <- bundle.objects) {
      // todo user-id
      val userlog = UserLog("user_id", bundle.id.toString(), stix.id.toString(), colPath, Timestamp.now().toString())
      for {
        userCol <- userLogF
        theError <- userCol.insert(userlog)
      } yield theError
    }
  }

  def saveLocalBundles(cyberList: List[CyberBundle]): Future[(MultiBulkWriteResult, MultiBulkWriteResult)] = {
    // the list of STIX bundles
    val bundleList = for (item <- cyberList) yield item.toStix
    // create the bundles info list
    // todo user-id
    val infoList = for (item <- cyberList) yield
      new BundleInfo("userx", item.id.value, item.name.value, Timestamp.now().toString())
    // insert all bundles and info
    for {
      bundles <- bundlesInfoF
      infErrors <- bundles.insert(ordered = true).many(infoList)
      bundles <- bundlesF
      errors <- bundles.insert(ordered = true).many(bundleList)
    } yield (infErrors, errors)
  }

  private def loadBundlesInfo(): Future[List[BundleInfo]] = for {
    bundleInfo <- bundlesInfoF
    theList <- bundleInfo.find(Json.obj()).
      cursor[BundleInfo](ReadPreference.nearest).
      collect[List](-1, Cursor.FailOnError[List[BundleInfo]]())
  } yield theList

  private def loadBundles(): Future[List[Bundle]] = for {
    bundles <- bundlesF
    bundleList <- bundles.find(Json.obj()).
      cursor[Bundle](ReadPreference.nearest).
      collect[List](-1, Cursor.FailOnError[List[Bundle]]())
  } yield bundleList

  def loadLocalBundles(): Future[List[CyberBundle]] = for {
    bundles <- loadBundles()
    infoList <- loadBundlesInfo()
    cyberBundles <- Future(for (bndl <- bundles) yield {
      val info = infoList.find(x => x.bundle_id == bndl.id.toString()).getOrElse(BundleInfo.emptyInfo())
      CyberBundle.fromStix(bndl, info.name)
    })
  } yield cyberBundles

  def dropLocalBundles(): Unit = {
    bundlesF.map(bndls => bndls.drop(true))
    bundlesInfoF.map(bndls => bndls.drop(true))
  }

}

