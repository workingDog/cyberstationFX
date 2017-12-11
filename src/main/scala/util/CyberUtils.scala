package util

import java.net.URL
import java.io.File

import scala.util.Random
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.{FileChooser, Stage}


object CyberUtils {

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
  def fileSelector(filter: Seq[String] = Seq("*.json", "*.zip")): Option[File] = {
    val fileChooser = new FileChooser {
      extensionFilters.add(new ExtensionFilter("bundle", filter))
    }
    Option(fileChooser.showOpenDialog(new Stage()))
  }
}
