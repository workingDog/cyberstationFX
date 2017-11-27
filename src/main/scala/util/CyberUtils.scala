package util

import java.net.URL

import com.kodekutters.stix._

import scala.util.Random



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

  // generate a 4 letters random name
  def randName: String = Random.alphanumeric.filter(_.isLetter).take(4).mkString

  // generate a 4 digit random name
  def randDigits: String = Random.alphanumeric.filter(_.isDigit).take(4).mkString

  val commonLabels = List[String]("anomalous-activity", "anonymization", "benign",
    "organization", "compromised", "malicious-activity", "attribution")

  // list of objects type names
  val listOfObjectTypes = Seq(
    AttackPattern.`type`,
    Identity.`type`,
    Campaign.`type`,
    CourseOfAction.`type`,
    Indicator.`type`,
    IntrusionSet.`type`,
    Malware.`type`,
    ObservedData.`type`,
    Report.`type`,
    ThreatActor.`type`,
    Tool.`type`,
    Vulnerability.`type`,
    MarkingDefinition.`type`,
    LanguageContent.`type`,
    Bundle.`type`)

}
