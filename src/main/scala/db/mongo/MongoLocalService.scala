package db.mongo

import java.io.File

import com.kodekutters.neo4j.Neo4jFileLoader.readBundle
import com.kodekutters.stix.StixObj._
import com.kodekutters.stix._
import com.typesafe.config.{Config, ConfigFactory}
import controllers.CyberStationControllerInterface
import cyber.{BundleInfo, CyberBundle}
import db.{DbService, UserLog}
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}
import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success}
import scalafx.scene.paint.Color

/**
  * the MongoDb Service support for storing local bundles data to a mongodb
  */
object MongoLocalService extends DbService {

  // needed for StixObj json write
  implicit val stixObjFormats = new OFormat[StixObj] {
    override def reads(json: JsValue): JsResult[StixObj] = fmt.reads(json)

    override def writes(o: StixObj): JsObject = fmt.writes(o).asInstanceOf[JsObject]
  }

  val count = mutable.Map("SDO" -> 0, "SRO" -> 0, "StixObj" -> 0)

  def resetCount(): Unit = count.foreach({ case (k, v) => count(k) = 0 })

  def inc(k: String): Unit = count(k) = count(k) + 1

  val config: Config = ConfigFactory.load

  var database: Future[DefaultDB] = _

  var isReady = false

  def isConnected() = isReady

  var dbUri = ""
  private var bundlesCol = "bundles"
  private var bundlesInf = "bundlesInfo"
  private var userLogCol = "userLog"
  private var timeout = 30 // seconds
  try {
    bundlesCol = config.getString("mongo.collection.bundles")
    bundlesInf = config.getString("mongo.collection.bundlesInfo")
    userLogCol = config.getString("mongo.collection.userLog")
    timeout = config.getInt("mongodb.timeout")
    dbUri = config.getString("mongodb.uri")
  } catch {
    case e: Throwable => println("---> config error: " + e)
  }

  def bundlesF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesCol))

  def bundlesInfoF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesInf))

  def userLogF: Future[JSONCollection] = database.map(_.collection[JSONCollection](userLogCol))

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
    Await.result(MongoLocalService.database, timeout seconds)
  }

  def close(): Unit = if (database != null && isReady) database.map(db => db.connection.close())

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
      stix match {
        case x: SDO => inc("SDO")
        case x: SRO => inc("SRO")
        case x: StixObj => inc("StixObj")
      }
      for {
        stxCol <- database.map(_.collection[JSONCollection](stix.`type`))
        theError <- stxCol.insert(stix)
      } yield theError
    }
  }

//  private def saveBundle(bundle: Bundle): Future[WriteResult] = for {
//    bundles <- bundlesF
//    lastError <- bundles.insert(bundle)
//  } yield lastError

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

  def saveFileToDB(file: File, controller: CyberStationControllerInterface): Unit = {
    controller.showSpinner(true)
    Future({
      println("---> MongoDb: " + dbUri)
      controller.showThis("---> saving: " + file.getName + " to MongoDb at: " + dbUri, Color.Black)
      if (file.getName.toLowerCase.endsWith(".zip")) {
        saveBundleZipFile(file)
      } else {
        saveBundleFile(file)
      }
      controller.showThis("Done saving: " + file.getName + " to MongoDb at: " + dbUri, Color.Black)
      controller.showThis("   SDO: " + count("SDO") + " SRO: " + count("SRO") + " StixObj: " + count("StixObj"), Color.Black)
      resetCount()
      controller.showSpinner(false)
    })
  }

  def saveBundleFile(file: File): Unit = {
    // read a STIX bundle from the file
    val jsondoc = Source.fromFile(file).mkString
    Option(Json.parse(jsondoc)) match {
      case None => println("\n-----> could not parse JSON in file: " + file.getName)
      case Some(js) =>
        // create a bundle object from it and convert its objects to nodes and relations
        Json.fromJson[Bundle](js).asOpt match {
          case None => println("\n-----> ERROR reading bundle in file: " + file.getName)
          case Some(bundle) => saveBundleAsStixs(bundle)
        }
        close()
    }
  }

  def saveBundleZipFile(file: File): Unit = {
    import scala.collection.JavaConverters._
    // get the zip file
    val rootZip = new java.util.zip.ZipFile(file)
    // for each entry file containing a single bundle
    rootZip.entries.asScala.foreach(f => {
      if (f.getName.toLowerCase.endsWith(".json") || f.getName.toLowerCase.endsWith(".stix")) {
        readBundle(rootZip.getInputStream(f)) match {
          case Some(bundle) => saveBundleAsStixs(bundle)
          case None => println("-----> ERROR invalid bundle JSON in zip file: \n")
        }
      }
    })
    close()
  }

}

