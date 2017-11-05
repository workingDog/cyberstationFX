package cyberProtocol

import com.kodekutters.stix._
import util.Utils

import scala.collection.mutable.ListBuffer
import scalafx.beans.property.{BooleanProperty, IntegerProperty, StringProperty}
import scalafx.collections.ObservableBuffer

/**
  * the common attributes of a sdo object
  */
trait CyberObj {
  val `type`: String
  var id: StringProperty
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

  def toStix: StixObj
}

/**
  * a STIX Bundle with a name
  *
  */
class CyberBundle() {
  val name = StringProperty("bundle " + Utils.randName)
  val `type`: String = Bundle.`type`
  var id: StringProperty = StringProperty(Identifier(Bundle.`type`).toString())
  val spec_version = StringProperty("2.0")
  var objects = ObservableBuffer[CyberObj]()

  def toStix = {
    val stixList = (for (obj <- objects) yield obj.toStix).to[ListBuffer]
    new Bundle(this.`type`, Identifier.stringToIdentifier(this.id.value), this.spec_version.value, stixList)
  }
}

// Indicator, ObservedData, Relationship, Sighting, LanguageContent, Bundle

class IndicatorForm() extends CyberObj {
  val `type`: String = Indicator.`type`
  var id: StringProperty = StringProperty(Identifier(Indicator.`type`).toString())

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
    Option(this.labels),
    Option(this.kill_chain_phases), Option(this.description.value),
    Option(this.revoked.value), Option(this.confidence.value),
    Option(this.external_references), Option(this.lang.value),
    Option(this.object_marking_refs), Option(this.granular_markings),
    None, None)
}