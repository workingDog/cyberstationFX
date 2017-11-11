package util

import java.net.URL

import scala.util.Random
import scalafx.collections.ObservableBuffer

object Utils {

  // check that the url is valid
  def urlValid(url: String): Boolean = {
    try {
      val checkUrl: URL = new URL(url) // this checks for the protocol
      checkUrl.toURI() // does the extra checking required for validation of URI
      true
    } catch {
      case x => false
    }
  }

  // determine if v is a case class
  def isCaseClass(v: Any): Boolean = {
    import reflect.runtime.universe._
    val typeMirror = runtimeMirror(v.getClass.getClassLoader)
    val instanceMirror = typeMirror.reflect(v)
    val symbol = instanceMirror.symbol
    symbol.isCaseClass
  }

  // generate a 4 letters random name
  def randName: String = Random.alphanumeric.filter(_.isLetter).take(4).mkString

  val initLabels = ObservableBuffer[String]("", "anomalous-activity", "anonymization", "benign",
    "organization", "compromised", "malicious-activity", "attribution")

}
