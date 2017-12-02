package db

import util.CyberUtils
import play.api.libs.json._
import com.kodekutters.stix.StixObj._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import com.kodekutters.stix._
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}
import reactivemongo.api._
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import com.typesafe.config.{Config, ConfigFactory}
import cyber.{BundleInfo, CyberBundle}

import scala.util.{Failure, Success}
import scala.language.{implicitConversions, postfixOps}
import scala.concurrent.duration._


/**
  * the MongoDbService support
  */
object MongoDbService extends DbService {

  // needed for StixObj json write
  implicit val stixObjFormats = new OFormat[StixObj] {
    override def reads(json: JsValue): JsResult[StixObj] = fmt.reads(json)

    override def writes(o: StixObj): JsObject = fmt.writes(o).asInstanceOf[JsObject]
  }

  val config: Config = ConfigFactory.load

  var database: Future[DefaultDB] = _

  var isReady = false

  def isConnected() = isReady

  private var bundlesCol = "bundles"
  private var bundlesInf = "bundlesInfo"
  private var userLogCol = "userLog"
  private var timeout = 30  // seconds
  try {
    bundlesCol = config.getString("mongo.collection.bundles")
    bundlesInf = config.getString("mongo.collection.bundlesInfo")
    userLogCol = config.getString("mongo.collection.userLog")
    timeout = config.getInt("mongodb.timeout")
  } catch {
    case e: Throwable => println("---> config error: " + e)
  }

  def bundlesF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesCol))

  def bundlesInfoF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesInf))

  def userLogF: Future[JSONCollection] = database.map(_.collection[JSONCollection](userLogCol))

  val dbUri = config.getString("mongodb.uri")

  /**
    * initialise this singleton
    */
  def init(): Unit = {
    try {
      println("trying to connect to: " + dbUri)
      val driver = new MongoDriver()
      database = for {
        uri <- Future.fromTry(MongoConnection.parseURI(dbUri))
        con = driver.connection(uri)
        dn <- Future(uri.db.get)
        db <- con.database(dn)
      } yield db
      database.onComplete {
        case Success(theDB) => isReady = true; println(s"mongodb connected to: $theDB")
        case Failure(err) => isReady = false; println(s"mongodb fail to connect, error: $err")
      }
    } catch {
      case ex: Throwable => isReady = false
    }
    // wait here for the connection to complete
    Await.result(MongoDbService.database, timeout seconds)
  }

  def close(): Unit = if(database != null && isReady) database.map(db => db.connection.close())

  /**
    * create all collections from the STIX objects type names (including Bundle)
    */
  private def createStixCollections(): Unit = {
    database.map(db => CyberUtils.listOfObjectTypes.foreach(objType => db.collection[JSONCollection](objType)))
  }

  def saveServerBundle(bundle: Bundle, colPath: String): Unit = {
    if (isConnected()) {
      // save the bundle of stix
      saveBundleAsStixs(bundle)
      // save the log to the db
      saveUserLog(bundle, colPath)
    }
  }

  private def saveUserLog(bundle: Bundle, colPath: String): Unit = {
    for (stix <- bundle.objects) {
      // todo user-id
      val userlog = UserLog("user_id", bundle.id.toString(), stix.id.toString(), colPath, Timestamp.now().toString())
      for {
        userCol <- userLogF
        theError <- userCol.insert(userlog)
      } yield theError
    }
  }

  private def saveBundleAsStixs(bundle: Bundle): Unit = {
    for (stix <- bundle.objects) {
      for {
        stxCol <- database.map(_.collection[JSONCollection](stix.`type`))
        theError <- stxCol.insert(stix)
      } yield theError
    }
  }

  private def saveBundle(bundle: Bundle): Future[WriteResult] = for {
    bundles <- bundlesF
    lastError <- bundles.insert(bundle)
  } yield lastError

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
      infErrors <- bundles.insert[BundleInfo](ordered = true).many(infoList)
      bundles <- bundlesF
      errors <- bundles.insert[Bundle](ordered = true).many(bundleList)
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

  private def dropBundles(): Unit = bundlesF.map(bndls => bndls.drop(true))

  private def dropBundlesInfo(): Unit = bundlesInfoF.map(bndls => bndls.drop(true))

  def dropLocalBundles(): Unit = {
    dropBundles()
    dropBundlesInfo()
  }

}

