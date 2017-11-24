package cyber

import com.kodekutters.stix._
import util.Utils

import scala.collection.mutable.ListBuffer
import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.{ObservableBuffer, ObservableMap}
import scala.collection.mutable


/**
  * representing the common attributes of an SDO as a set of properties, a form
  */
trait CyberObj {
  val `type` = StringProperty("")
  val id = StringProperty("")
  val name = StringProperty("")
  val lang = StringProperty("en")
  val created = StringProperty(Timestamp.now().toString())
  val modified = StringProperty(Timestamp.now().toString())
  val created_by_ref = StringProperty("")
  val revoked = BooleanProperty(false)
  val labels = mutable.Set[String]()
  val confidence = StringProperty("0")
  val external_references = ObservableBuffer[ExternalRefForm]()
  val object_marking_refs = ObservableBuffer[String]() // List[Identifier]
  val granular_markings = ObservableBuffer[String]() // List[GranularMarking]
  // to convert into the Stix object that the Cyber Object represents
  def toStix: StixObj
}

/**
  * a Bundle form
  */
class CyberBundle() {
  val name = StringProperty("bundle_" + Utils.randDigits)
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
  name.value = "indicator_" + Utils.randDigits

  val pattern = StringProperty("")
  val valid_from = StringProperty(Timestamp.now().toString())
  val valid_until = StringProperty("")
  val kill_chain_phases = mutable.ListBuffer[KillChainPhase]()
  val description = StringProperty("")

  def toStix = new Indicator(
    Indicator.`type`, Identifier.stringToIdentifier(id.value),
    Timestamp(created.value), Timestamp(modified.value), pattern.value,
    Timestamp(valid_from.value), Option(name.value), Option(Timestamp(valid_until.value)),
    Option(labels.toList), Option(kill_chain_phases.toList), Option(description.value),
    Option(revoked.value),
    Option(if (confidence.value.isEmpty) 0 else Integer.parseInt(confidence.value)),
    ExternalRefForm.toExternalRefListOpt(external_references), Option(lang.value),
    IndicatorForm.toIdentifierListOpt(object_marking_refs), Option(List()),
    IndicatorForm.toIdentifierOpt(created_by_ref.value), None)

}

object IndicatorForm {

  def toIdentifierOpt(s: String): Option[Identifier] = {
    if (s == null || s.isEmpty) None
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

  def toIdentifierListOpt(theList: ObservableBuffer[String]): Option[List[Identifier]] = {
    if (theList == null)
      None
    else
      Option((for (s <- theList) yield toIdentifier(s)).toList)
  }

  def fromIdentifierList(theList: List[Identifier]): ObservableBuffer[String] = {
    if (theList == null)
      ObservableBuffer[String]()
    else
      (for (s <- theList) yield s.toString()).to[ObservableBuffer]
  }


}

/**
  * an ExternalReference form
  */
class ExternalRefForm() {
  val source_name = StringProperty("")
  val description = StringProperty("")
  val url = StringProperty("")
  val external_id = StringProperty("")
  val hashes = ObservableMap.empty[String, String]

  /*
    hashes.onChange((map, change) => {
    println("hashes = " + hashes.mkString("[", ", ", "]"))
    println(prettyChange(change))
  })

  hashes("SHA256") = "something"
   */

  def toStix = new ExternalReference(
    source_name = source_name.value,
    Option(description.value),
    Option(external_id.value),
    Option(url.value),
    Option(hashes.toMap))

}

object ExternalRefForm {

  def fromStix(stix: ExternalReference): ExternalRefForm = {
    new ExternalRefForm() {
      source_name.value = stix.source_name
      description.value = stix.description.getOrElse("")
      url.value = stix.url.getOrElse("")
      external_id.value = stix.external_id.getOrElse("")
      //  hashes = ObservableMap.empty[String, String]
    }
  }

  def toExternalRefListOpt(theList: ObservableBuffer[ExternalRefForm]): Option[List[ExternalReference]] = {
    if (theList == null)
      None
    else
      Option((for (s <- theList) yield s.toStix).toList)
  }

  def fromExternalRefList(theList: List[ExternalReference]): ObservableBuffer[ExternalRefForm] = {
    if (theList == null)
      ObservableBuffer[ExternalRefForm]()
    else
      (for (s <- theList) yield ExternalRefForm.fromStix(s)).to[ObservableBuffer]
  }

}

/**
  * conversions utilities
  */
object CyberConverter {

  /**
    * convert a StixObj into a corresponding CyberObj
    *
    * @param theStix
    */
  def toCyberObj(theStix: StixObj): CyberObj = {
    theStix match {
      case stix: Indicator => new IndicatorForm {
        `type`.value = stix.`type`
        id.value = stix.id.toString()
        name.value = stix.name.getOrElse("")
        created.value = stix.created.toString()
        modified.value = stix.modified.toString()
        lang.value = stix.lang.getOrElse("")
        confidence.value = stix.confidence.getOrElse(0).toString
        labels ++= stix.labels.getOrElse(List())
        created_by_ref.value = stix.created_by_ref.getOrElse("").toString
        revoked.value = stix.revoked.getOrElse(false)
        external_references ++= ExternalRefForm.fromExternalRefList(stix.external_references.getOrElse(List()))
        object_marking_refs ++= IndicatorForm.fromIdentifierList(stix.object_marking_refs.getOrElse(List()))
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

case class LabelItem(init: Boolean, name: String, var form: CyberObj) {
  val selected = BooleanProperty(init)
  selected.onChange { (_, _, newValue) =>
    if (form != null)
      if (newValue) form.labels += name else form.labels -= name
  }

  override def toString: String = name
}

