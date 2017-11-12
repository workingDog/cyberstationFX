package cyber

import com.kodekutters.stix._
import util.Utils

import scala.collection.mutable.ListBuffer
import scalafx.beans.property.{BooleanProperty, IntegerProperty, StringProperty}
import scalafx.collections.ObservableBuffer

import scala.collection.mutable


/**
  * representing the common attributes of an SDO as a set of properties, a form
  */
trait CyberObj {
  val `type` = StringProperty("")
  val id = StringProperty("")
  val name = StringProperty("")
  val lang = StringProperty("")
  val created = StringProperty(Timestamp.now().toString())
  val modified = StringProperty("")
  val created_by_ref = StringProperty("")
  val revoked = BooleanProperty(false)
  val labels = mutable.Set[String]()
  val confidence = IntegerProperty(0)
  val external_references = ObservableBuffer[String]() // List[ExternalReference]
  val object_marking_refs = ObservableBuffer[String]() // List[Identifier]
  val granular_markings = ObservableBuffer[String]() // List[GranularMarking]
  // to get the Stix object of the Cyber Object
  def toStix: StixObj
}

/**
  * a Bundle form
  */
class CyberBundle() {
  val name = StringProperty("bundle " + Utils.randName)
  val `type`: StringProperty = StringProperty(Bundle.`type`)
  var id: StringProperty = StringProperty(Identifier(Bundle.`type`).toString())
  val spec_version = StringProperty("2.0")
  var objects = ObservableBuffer[CyberObj]()

  def toStix = {
    val stixList = (for (obj <- objects) yield obj.toStix).to[ListBuffer]
    new Bundle(this.`type`.value, Identifier.stringToIdentifier(this.id.value), this.spec_version.value, stixList)
  }
}

// Indicator, ObservedData, Relationship, Sighting, LanguageContent, Bundle

/**
  * an Indicator form
  */
class IndicatorForm() extends CyberObj {
  `type`.value = Indicator.`type`
  id.value = Identifier(Indicator.`type`).toString()
  name.value = "indicator " + Utils.randName

  val pattern = StringProperty("")
  val valid_from = StringProperty("")
  val valid_until = StringProperty("")
  val kill_chain_phases = List[KillChainPhase]()
  val description = StringProperty("")

  def toStix = new Indicator(Indicator.`type`, Identifier.stringToIdentifier(id.value),
    Timestamp(created.value), Timestamp(modified.value), pattern.value,
    Timestamp(valid_from.value), Option(name.value), Option(Timestamp(valid_until.value)),
    Option(labels.toList), Option(kill_chain_phases), Option(description.value),
    Option(revoked.value), Option(confidence.value),
    Option(List()), Option(lang.value),
    Option(List()), Option(List()),
    Option(Identifier.stringToIdentifier(created_by_ref.value)), None)
}

/**
  * convert a StixObj into a corresponding CyberObj
  */
object CyberConverter {

  /**
    * convert a StixObj into a corresponding CyberObj
    * @param theStix
    */
  def toCyberObj(theStix: StixObj): CyberObj = {
    theStix match {
      case stix: Indicator => new IndicatorForm {
        `type`.value = stix.`type`
        id.value = stix.id.toString()
        name.value = stix.name.getOrElse("")
        created.value = stix.created.toString()
        lang.value = stix.lang.getOrElse("")
        stix.labels.getOrElse(List()).foreach(lbl => labels +: lbl)
      }
      case stix: AttackPattern => new IndicatorForm()
      case stix: Identity => new IndicatorForm()
      case stix: Campaign => new IndicatorForm()
      case stix: CourseOfAction => new IndicatorForm()
      case stix: IntrusionSet => new IndicatorForm()
      case stix: Malware => new IndicatorForm()
      case stix: ObservedData => new IndicatorForm()
      case stix: Report => new IndicatorForm()
      case stix: ThreatActor => new IndicatorForm()
      case stix: Tool => new IndicatorForm()
      case stix: Vulnerability => new IndicatorForm()
      case stix: Relationship => new IndicatorForm()
      case stix: Sighting => new IndicatorForm()
      case stix: MarkingDefinition => new IndicatorForm()
      case stix: LanguageContent => new IndicatorForm()
      case _ => new IndicatorForm()
    }
  }

}
