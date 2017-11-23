package util

import java.net.URL

import com.kodekutters.stix._
import cyber.ExternalRefForm

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
    LanguageContent.`type`)
  //  ExternalReference.`type`,
  //  KillChainPhase.`type`,
  //  GranularMarking.`type`)  // Identifier.`type`


  def toIdentifierOpt(s: String): Option[Identifier] = {
    if (s.isEmpty) None
    else {
      val part = s.split("--")
      if (part(0).isEmpty) None
      else if (part(1).isEmpty) None
      else Option(new Identifier(part(0), part(1)))
    }
  }

  def toIdentifier(s: String): Identifier = {
    val part = s.split("--")
    new Identifier(part(0), part(1))
  }

  def toIdentifierList(theList: ObservableBuffer[String]): List[Identifier] =
    (for (s <- theList) yield toIdentifier(s)).toList

  def fromIdentifierList(theList: List[Identifier]): ObservableBuffer[String] =
    (for (s <- theList) yield s.toString()).to[ObservableBuffer]

  def fromExternalRefList(theList: List[ExternalReference]): ObservableBuffer[ExternalRefForm] =
    (for (s <- theList) yield ExternalRefForm.fromStix(s)).to[ObservableBuffer]


}
