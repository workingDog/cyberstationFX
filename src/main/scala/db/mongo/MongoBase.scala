package db.mongo

import java.io.File

import com.kodekutters.neo4j.Neo4jFileLoader.readBundle
import com.kodekutters.stix.StixObj._
import com.kodekutters.stix._
import com.typesafe.config.{Config, ConfigFactory}
import controllers.CyberStationControllerInterface
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.play.json.collection._
import support.Counter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success}
import scalafx.scene.paint.Color

/**
  * the MongoDb base support for storing data to a mongodb
  */
trait MongoBase {

  // needed for StixObj json write
  implicit val stixObjFormats = new OFormat[StixObj] {
    override def reads(json: JsValue): JsResult[StixObj] = fmt.reads(json)

    override def writes(o: StixObj): JsObject = fmt.writes(o).asInstanceOf[JsObject]
  }

  val counter = Counter()

  val config: Config = ConfigFactory.load

  var database: Future[DefaultDB] = _

  var isReady = false

  def isConnected(): Boolean = isReady

  var dbUri = ""

  var timeout = 30 // seconds

  def readConfig(): Unit = { }

  /**
    * initialisation --> config and connection
    */
  def init(): Unit = {
    readConfig()
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
    Await.result(database, timeout seconds)
  }

  def close(): Unit = if (database != null && isReady) database.map(db => db.connection.close())

  def saveBundleAsStixs(bundle: Bundle): Unit = {
    for (stix <- bundle.objects) {
      counter.countStix(stix)
      for {
        stxCol <- database.map(_.collection[JSONCollection](stix.`type`))
        theError <- stxCol.insert(stix)
      } yield theError
    }
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
      controller.showThis("   SDO: " + counter.count("SDO") + " SRO: " + counter.count("SRO") + " StixObj: " + counter.count("StixObj"), Color.Black)
      counter.reset()
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

