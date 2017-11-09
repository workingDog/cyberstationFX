package cyber

import com.kodekutters.stix._
import util.Utils

import scala.collection.mutable.ListBuffer
import scalafx.beans.property.{BooleanProperty, IntegerProperty, StringProperty}
import scalafx.collections.ObservableBuffer

/**
  * representing the common attributes of an sdo as a set of properties
  */
trait CyberObj {
  val `type`: StringProperty
  val id: StringProperty
  val name: StringProperty
  val lang: StringProperty
  val created: StringProperty
  val modified: StringProperty
  val created_by_ref: StringProperty
  val revoked: BooleanProperty
  val labels: List[String]
  val confidence: IntegerProperty
  val external_references: List[ExternalReference]
  val object_marking_refs: List[Identifier]
  val granular_markings: List[GranularMarking]

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
  val `type`: StringProperty = StringProperty(Indicator.`type`)
  val id: StringProperty = StringProperty(Identifier(Indicator.`type`).toString())

  val lang = StringProperty("")
  val created = StringProperty(Timestamp.now().toString())
  val modified = StringProperty("")
  val created_by_ref = StringProperty("")
  val revoked = BooleanProperty(false)

  val labels: List[String] = List()
  val confidence = IntegerProperty(0)
  val external_references: List[ExternalReference] = List()
  val object_marking_refs: List[Identifier] = List()
  val granular_markings: List[GranularMarking] = List()

  val pattern = StringProperty("")
  val valid_from = StringProperty("")
  val name = StringProperty("indicator " + Utils.randName)
  val valid_until = StringProperty("")
  val kill_chain_phases = List[KillChainPhase]()
  val description = StringProperty("")

  def toStix = new Indicator(Indicator.`type`, Identifier.stringToIdentifier(this.id.value),
    Timestamp(this.created.value), Timestamp(this.modified.value), this.pattern.value,
    Timestamp(this.valid_from.value), Option(this.name.value), Option(Timestamp(this.valid_until.value)),
    Option(this.labels), Option(this.kill_chain_phases), Option(this.description.value),
    Option(this.revoked.value), Option(this.confidence.value),
    Option(this.external_references), Option(this.lang.value),
    Option(this.object_marking_refs), Option(this.granular_markings),
    Option(Identifier.stringToIdentifier(this.created_by_ref.value)), None)
}

/**
  * convert a StixObj into a corresponding CyberObj
  */
object CyberConverter {

  def toCyberObj(theStix: StixObj): CyberObj = {
    theStix match {
      case stix: Indicator =>
        new IndicatorForm {
          `type`.value = stix.`type`
          id.value = stix.id.toString()
          name.value = stix.name.getOrElse("")
          created.value = stix.created.toString()
          lang.value = stix.lang.getOrElse("")
        }

      case stix: Identity => new IndicatorForm()
      case stix: Campaign => new IndicatorForm()
    }
  }

}
