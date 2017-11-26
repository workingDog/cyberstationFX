package db

import util.Utils
import play.api.libs.json._
import com.kodekutters.stix.StixObj._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.kodekutters.stix._
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}
import reactivemongo.api._
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import com.typesafe.config.{Config, ConfigFactory}
import cyber.{BundleInfo, CyberBundle}

import scala.util.{Failure, Success}


/**
  * the MongoDbService support
  */
object MongoDbService {

  // needed for StixObj json write
  implicit val stixObjFormats = new OFormat[StixObj] {
    override def reads(json: JsValue): JsResult[StixObj] = fmt.reads(json)

    override def writes(o: StixObj): JsObject = fmt.writes(o).asInstanceOf[JsObject]
  }

  val config: Config = ConfigFactory.load

  var database: Future[DefaultDB] = _

  var hasDB = false

  private var bundlesCol = "bundles"
  private var bundlesInf = "bundlesInfo"
  private var userLogCol = "userLog"
  try {
    bundlesCol = config.getString("mongo.collection.bundles")
    bundlesInf = config.getString("mongo.collection.bundlesInfo")
    userLogCol = config.getString("mongo.collection.userLog")
  } catch {
    case e: Throwable => println("---> config error: " + e)
  }

  def bundlesF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesCol))

  def bundlesInfoF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesInf))

  def userLogF: Future[JSONCollection] = database.map(_.collection[JSONCollection](userLogCol))

  val mongoUri = config.getString("mongodb.uri")

  /**
    * initialise this singleton
    */
  def init(): Unit = {
    println("trying to connect to: " + mongoUri)
    val driver = new MongoDriver()
    database = for {
      uri <- Future.fromTry(MongoConnection.parseURI(mongoUri))
      con = driver.connection(uri)
      dn <- Future(uri.db.get)
      db <- con.database(dn)
    } yield db
    database.onComplete {
      case Success(theDB) => println(s"mongodb connected to: $theDB")
      case Failure(err) => println(s"mongodb fail to connect, error: $err")
    }
  }

  def close(): Unit = database.map(db => db.connection.close())

  def hasDB(hasIt: Boolean): Unit = hasDB = hasIt

  /**
    * create all collections from the STIX objects type names (including Bundle)
    */
  def createStixCollections(): Unit = {
    database.map(db => Utils.listOfObjectTypes.foreach(objType => db.collection[JSONCollection](objType)))
  }

  def saveServerSent(bundle: Bundle, colPath: String): Unit = {
    if (hasDB) {
      // save the bundle of stix
      saveBundleAsStixs(bundle)
      // save the log to the db
      saveUserLog(bundle, colPath)
    }
  }

  def saveUserLog(bundle: Bundle, colPath: String): Unit = {
    for (stix <- bundle.objects) {
      // todo user-id
      val userlog = UserLog("user_id", bundle.id.toString(), stix.id.toString(), Timestamp.now().toString(), colPath)
      for {
        userCol <- userLogF
        theError <- userCol.insert(userlog)
      } yield theError
    }
  }

  def saveBundleAsStixs(bundle: Bundle): Unit = {
    for (stix <- bundle.objects) {
      for {
        stxCol <- database.map(_.collection[JSONCollection](stix.`type`))
        theError <- stxCol.insert(stix)
      } yield theError
    }
  }

  def saveBundle(bundle: Bundle): Future[WriteResult] = for {
    bundles <- bundlesF
    lastError <- bundles.insert(bundle)
  } yield lastError

  def saveAllBundles(cyberList: List[CyberBundle]): Future[(MultiBulkWriteResult, MultiBulkWriteResult)] = {
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

  def loadBundlesInfo(): Future[List[BundleInfo]] = for {
    bundleInfo <- bundlesInfoF
    theList <- bundleInfo.find(Json.obj()).
      cursor[BundleInfo](ReadPreference.nearest).
      collect[List](-1, Cursor.FailOnError[List[BundleInfo]]())
  } yield theList

  def loadBundles(): Future[List[Bundle]] = for {
    bundles <- bundlesF
    bundleList <- bundles.find(Json.obj()).
      cursor[Bundle](ReadPreference.nearest).
      collect[List](-1, Cursor.FailOnError[List[Bundle]]())
  } yield bundleList

  def loadCyberBundles(): Future[List[CyberBundle]] = for {
    bundles <- loadBundles()
    infoList <- loadBundlesInfo()
    cyberBundles <- Future(for (bndl <- bundles) yield {
      val info = infoList.find(x => x.bundle_id == bndl.id.toString()).getOrElse(BundleInfo.emptyInfo())
      CyberBundle.fromStix(bndl, info.name)
    })
  } yield cyberBundles

  def dropBundles(): Unit = bundlesF.map(bndls => bndls.drop(true))

  def dropBundlesInfo(): Unit = bundlesInfoF.map(bndls => bndls.drop(true))

  def dropAllBundles(): Unit = {
    dropBundles()
    dropBundlesInfo()
  }

}

