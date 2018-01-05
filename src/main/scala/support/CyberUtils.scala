package support

import java.net.{URI, URL}
import java.io.{File, IOException, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import java.util.UUID

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

  // generate a long random name
  def randNameLong: String = Random.alphanumeric.filter(_.isLetter).take(16).mkString

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

  def makeTempDir(dirName: String): Path = {
    try {
      //  val thePath = Paths.get(URI.create(new java.io.File(".").getCanonicalPath + "/" + name))
      // creates temporary directory
      val dir = Files.createTempDirectory(dirName)
      println("----> temp dir: " + dir.toString)
      // delete directory when the virtual machine terminate
      dir.toFile.deleteOnExit()
      dir
    } catch {
      case ex: Exception => ex.printStackTrace(); null
    }
  }

  /**
    * write a bundle in json string representation to the output file
    *
    * @param outFile  the output file to write to
    * @param bundlejs a string representing a bundle in json format
    */
  def writeToFile(outFile: File, bundlejs: String): Unit = {
    val writer = new PrintWriter(outFile)
    try {
      writer.write(bundlejs)
    } catch {
      case e: IOException => e.printStackTrace()
    }
    finally {
      writer.close()
    }
  }
}
