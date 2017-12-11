package db.mongo

import java.io.File

import com.kodekutters.stix.StixObj._
import com.kodekutters.stix._
import com.typesafe.config.{Config, ConfigFactory}
import controllers.CyberStationControllerInterface
import db.neo4j.MakerSupport.loadBundle
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.play.json.collection._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success}
import scalafx.scene.paint.Color

/**
  * the MongoDbStix for saving files to mongodb
  */
object MongoDbStix {

  // needed for StixObj json write
  implicit val stixObjFormats = new OFormat[StixObj] {
    override def reads(json: JsValue): JsResult[StixObj] = fmt.reads(json)

    override def writes(o: StixObj): JsObject = fmt.writes(o).asInstanceOf[JsObject]
  }

  val config: Config = ConfigFactory.load

  var database: Future[DefaultDB] = _

  var isReady = false

  def isConnected() = isReady

  var dbUri = ""
  private var timeout = 30 // seconds
  try {
    timeout = config.getInt("mongodbStix.timeout")
    dbUri = config.getString("mongodbStix.uri")
  } catch {
    case e: Throwable => println("---> config error: " + e)
  }

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
    Await.result(database, timeout seconds)
  }

  def close(): Unit = if (database != null && isReady) database.map(db => db.connection.close())

  // all non-STIX-2 types are put in the designated "custom-object" collection
  private def saveBundleAsStixs(bundle: Bundle): Unit = {
    for (stix <- bundle.objects) {
      val stixType = if (stix.`type`.startsWith("x-")) "custom-object" else stix.`type`
      for {
        stxCol <- database.map(_.collection[JSONCollection](stixType))
        theError <- stxCol.insert(stix)
      } yield theError
    }
  }

  def saveFileToDB(file: File, controller: CyberStationControllerInterface): Unit = {
    if (isConnected()) {
      controller.showSpinner(true)
      controller.showThis("Trying to connect to database: " + dbUri, Color.Black)
      Future(try {
        controller.showThis("---> saving: " + file.getName + " to MongoDb at: " + dbUri, Color.Black)
        if (file.getName.toLowerCase.endsWith(".json")) saveBundleFile(file)
        if (file.getName.toLowerCase.endsWith(".zip")) saveBundleZipFile(file)
        controller.showThis("Done saving: " + file.getName + " to MongoDb at: " + dbUri, Color.Black)
        println("----> Done saving: " + file.getName + " to MongoDb at: " + dbUri)
      } catch {
        case ex: Throwable =>
          controller.showThis("Fail to connect to database: " + dbUri + " --> data is not be saved to the database", Color.Red)
      } finally {
        controller.showSpinner(false)
      })
    } else {
      controller.showThis("Mongo database: " + dbUri + " is not connected", Color.Red)
      println("----> Mongo database: " + dbUri + " is not connected, no saving done")
    }
  }

  def saveBundleFile(file: File): Unit = {
    // read a STIX bundle from the file
    val jsondoc = Source.fromFile(file).mkString
    Option(Json.parse(jsondoc)) match {
      case None => println("-----> could not parse JSON in file: " + file.getName)
      case Some(js) =>
        // create a bundle object from it and convert its objects to nodes and relations
        Json.fromJson[Bundle](js).asOpt match {
          case None => println("-----> ERROR reading bundle in file: " + file.getName)
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
    rootZip.entries.asScala.filter(_.getName.toLowerCase.endsWith(".json")).foreach(f => {
      loadBundle(rootZip.getInputStream(f)) match {
        case Some(bundle) => saveBundleAsStixs(bundle)
        case None => println("-----> ERROR invalid bundle JSON in zip file: \n")
      }
    })
    close()
  }

}

