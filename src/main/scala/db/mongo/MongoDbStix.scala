package db.mongo

import java.io.File
import com.kodekutters.stix._
import controllers.CyberStationControllerInterface
import db.neo4j.Neo4jService
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.play.json.collection._
import reactivemongo.play.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}


/**
  * the MongoDbStix for saving STIX-2 objects to a mongodb
  */
object MongoDbStix extends MongoBase {

  val customObjectType = "custom-object"

  private var readTimeout = 60 // seconds

  override def readConfig(): Unit = {
    try {
      readTimeout = config.getInt("mongodbStix.readTimeout")
      timeout = config.getInt("mongodbStix.timeout")
      dbUri = config.getString("mongodbStix.uri")
    } catch {
      case e: Throwable => println("---> config error: " + e)
    }
  }

  def writeToMongo(file: File, bundle: Bundle): Unit = saveBundleAsStixs(bundle)

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
      val seqListOfStix = Await.result(Future.sequence(readAllStix()), Duration(readTimeout, SECONDS))
      // do the saving to Neo4j
      Neo4jService.saveStixToNeo4j(seqListOfStix.flatten.toList, controller)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

}

