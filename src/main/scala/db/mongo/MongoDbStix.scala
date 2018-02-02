package db.mongo

import java.io.File

import com.kodekutters.stix.StixObj._
import com.kodekutters.stix._
import com.typesafe.config.{Config, ConfigFactory}
import controllers.CyberStationControllerInterface
import com.kodekutters.neo4j.Neo4jFileLoader.readBundle
import db.neo4j.Neo4jService
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.play.json.collection._
import reactivemongo.play.json._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success}
import scalafx.scene.paint.Color


/**
  * the MongoDbStix for saving STIX-2 objects to a mongodb
  */
object MongoDbStix {

  // needed for StixObj json write
  implicit val stixObjFormats = new OFormat[StixObj] {
    override def reads(json: JsValue): JsResult[StixObj] = fmt.reads(json)

    override def writes(o: StixObj): JsObject = fmt.writes(o).asInstanceOf[JsObject]
  }

  val count = mutable.Map("SDO" -> 0, "SRO" -> 0, "StixObj" -> 0)

  def resetCount(): Unit = count.foreach({ case (k, v) => count(k) = 0 })

  def inc(k: String): Unit = count(k) = count(k) + 1

  val customObjectType = "custom-object"

  val config: Config = ConfigFactory.load

  var database: Future[DefaultDB] = _

  var isReady = false

  def isConnected() = isReady

  var dbUri = ""
  private var timeout = 30 // seconds
  private var readTimeout = 60 // seconds
  try {
    readTimeout = config.getInt("mongodbStix.readTimeout")
    timeout = config.getInt("mongodbStix.timeout")
    dbUri = config.getString("mongodbStix.uri")
  } catch {
    case e: Throwable => println("---> config error: " + e)
  }

  // duration allowed for reading all STIX from mongo
  private var readDuration = Duration(readTimeout, SECONDS)

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
      val stixType = if (stix.`type`.startsWith("x-")) customObjectType else stix.`type`
      stix match {
        case x: SDO => inc("SDO")
        case x: SRO => inc("SRO")
        case x: StixObj => inc("StixObj")
      }
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
        if (file.getName.toLowerCase.endsWith(".zip")) {
          saveBundleZipFile(file)
        } else {
          saveBundleFile(file)
        }
        controller.showThis("Done saving: " + file.getName + " to MongoDb at: " + dbUri, Color.Black)
        controller.showThis("   SDO: " + count("SDO") + " SRO: " + count("SRO") + " StixObj: " + count("StixObj"), Color.Black)
        resetCount()
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

  /**
    * reads all STIX-2 objects, each from its own mongoDB collection
    *
    * @return a sequence of futures of lists of STIX-2 objects
    */
  def readAllStix(): Seq[Future[List[StixObj]]] = {
    val allTypes = Util.listOfSDOTypes ++ Util.listOfSROTypes ++ Util.listOfStixTypes ++ List(LanguageContent.`type`, customObjectType)
    for (stixType <- allTypes) yield {
      for {
        stxCol <- database.map(_.collection[JSONCollection](stixType))
        theList <- stxCol.find(Json.obj()).
          cursor[StixObj](ReadPreference.nearest).
          collect[List](-1, Cursor.FailOnError[List[StixObj]]())
      } yield theList
    }
  }

  def saveMongoToNeo4j(controller: CyberStationControllerInterface): Unit = {
    try {
      // wait for all readings to be completed
      val seqListOfStix = Await.result(Future.sequence(readAllStix()), readDuration)
      // do the saving to Neo4j
      Neo4jService.saveStixToNeo4j(seqListOfStix.flatten.toList, controller)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

}

