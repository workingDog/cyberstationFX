package support

import java.net.URL
import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._

import scala.concurrent.Future
import scala.util.Random
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.{FileChooser, Stage}

import scala.concurrent.ExecutionContext.Implicits.global


object CyberUtils {
  // create an Akka system for thread and streaming management
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  // check that the url is valid
  def urlValid(url: String): Boolean = {
    try {
      val checkUrl: URL = new URL(url) // this checks for the protocol
      checkUrl.toURI() // does the extra checking required for validation of URI
      true
    } catch {
      case x: Throwable => false
    }
  }

  // generate a 4-lettes random name
  def randName: String = Random.alphanumeric.filter(_.isLetter).take(4).mkString

  // generate a 4-digits random name
  def randDigits: String = Random.alphanumeric.filter(_.isDigit).take(4).mkString

  val commonLabels = List[String]("anomalous-activity", "anonymization", "benign",
    "organization", "compromised", "malicious-activity", "attribution")

  /**
    * popup a open fileChooser with the desired filter, default .json and .zip
    */
  def fileSelector(filter: Seq[String] = Seq("*.*", "*.zip")): Option[File] = {
    val fileChooser = new FileChooser {
      extensionFilters.add(new ExtensionFilter("bundle", filter))
    }
    Option(fileChooser.showOpenDialog(new Stage()))
  }

  /**
    * get some json data from a network feed
    *
    * @param thePath the path of the data
    * @return a Future[JsValue]
    */
  def getDataFrom(thePath: String): Future[JsValue] = {
    val wsClient = StandaloneAhcWSClient()
    wsClient.url(thePath).get().map { response =>
      response.status match {
        case 200 => response.body[JsValue]
        case x => println("----> response.status: " + x); JsNull
      }
    }.recover({
      case e: Exception => println("----> could not connect to: " + thePath); JsNull
    })
  }

}
