package cyber

import com.jfoenix.controls.JFXButton
import com.kodekutters.stix.{Bundle, _}
import play.api.libs.json.Json
import support.CyberUtils

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
  val created = StringProperty(Timestamp.now().toString())
  val modified = StringProperty(Timestamp.now().toString())
  val created_by_ref = StringProperty("")
  val revoked = BooleanProperty(false)
  val labels = mutable.Set[String]()
  val external_references = ObservableBuffer[ExternalRefForm]()
  val object_marking_refs = ObservableBuffer[String]() // List[Identifier]
  val granular_markings = ObservableBuffer[String]() // todo List[GranularMarking]
  // convert to the associated Stix object
  def toStix: StixObj
}

/**
  * a Cyber Bundle form
  */
class CyberBundle() {
  val name = StringProperty("bundle_" + CyberUtils.randDigits)
  val `type`: StringProperty = StringProperty(Bundle.`type`)
  var id: StringProperty = StringProperty(Identifier(Bundle.`type`).toString())
  val spec_version = StringProperty("2.0")
  var objects = ObservableBuffer[CyberObj]()

  def toStix = {
    val stixList = (for (obj <- objects) yield obj.toStix).to[ListBuffer]
    new Bundle(this.`type`.value, Identifier.stringToIdentifier(this.id.value), this.spec_version.value, stixList)
  }
}

object CyberBundle {

  def fromStix(stix: Bundle, bndlName: String = "no-name"): CyberBundle = new CyberBundle() {
    name.value = bndlName
    `type`.value = stix.`type`
    id.value = stix.id.toString()
    spec_version.value = stix.spec_version
    objects ++= (for (obj <- stix.objects) yield CyberConverter.toCyberObj(obj))
  }

}

/**
  * to store the bundle name and extra info
  *
  * @param user_id   the user id of this session
  * @param bundle_id the bundle id stored
  * @param name      the name of the bundle
  * @param timestamp the ime at which this info was put in the database
  */
case class BundleInfo(user_id: String, bundle_id: String, name: String, timestamp: String)

object BundleInfo {
  val `type` = "bundleInfo"
  implicit val fmt = Json.format[BundleInfo]

  def emptyInfo() = new BundleInfo("", "", "", "")
}


/**
  * an CustomStix form
  */
class CustomStixForm() extends CyberObj {
  `type`.value = CustomStix.`type`
  id.value = Identifier(CustomStix.`type`).toString()
  name.value = "x-custom-" + CyberUtils.randDigits

  def toStix = new CustomStix(
    `type`.value, Identifier.stringToIdentifier(id.value),
    Timestamp(created.value), Timestamp(modified.value),
    Option(revoked.value),
    Option(labels.toList),
    ExternalRefForm.toExternalRefListOpt(external_references),
    CyberConverter.toIdentifierListOpt(object_marking_refs), Option(List()),
    CyberConverter.toIdentifierOpt(created_by_ref.value), None
  )

}

object CustomStixForm {

  def clone(inForm: CustomStixForm) = {
    new CustomStixForm {
      `type`.value = inForm.`type`.value
      id.value = inForm.id.value
      name.value = inForm.name.value
      created.value = inForm.created.value
      modified.value = inForm.modified.value
      labels ++ inForm.labels
      created_by_ref.value = inForm.created_by_ref.value
      revoked.value = inForm.revoked.value
      external_references ++ inForm.external_references
      object_marking_refs ++ inForm.object_marking_refs
    }
  }

  def fromStix(stix: CustomStix): CustomStixForm = new CustomStixForm {
    `type`.value = stix.`type`
    id.value = stix.id.toString()
    name.value = "x-custom-" + CyberUtils.randDigits
    created.value = stix.created.toString()
    modified.value = stix.modified.toString()
    labels ++= stix.labels.getOrElse(List())
    created_by_ref.value = stix.created_by_ref.getOrElse("").toString
    revoked.value = stix.revoked.getOrElse(false)
    external_references ++= ExternalRefForm.fromExternalRefList(stix.external_references.getOrElse(List()))
    object_marking_refs ++= CyberConverter.fromIdentifierList(stix.object_marking_refs.getOrElse(List()))
  }

}

// Indicator, ObservedData, Relationship, Sighting, LanguageContent, Bundle

/**
  * an Indicator form
  */
class IndicatorForm() extends CyberObj {
  `type`.value = Indicator.`type`
  id.value = Identifier(Indicator.`type`).toString()
  name.value = "indicator_" + CyberUtils.randDigits

  val pattern = StringProperty("")
  val valid_from = StringProperty(Timestamp.now().toString())
  val valid_until = StringProperty("")
  val kill_chain_phases = ObservableBuffer[KillChainPhaseForm]()
  val description = StringProperty("")

  def toStix = new Indicator(
    Indicator.`type`, Identifier.stringToIdentifier(id.value),
    Timestamp(created.value), Timestamp(modified.value), pattern.value,
    Timestamp(valid_from.value), Option(name.value), Option(Timestamp(valid_until.value)),
    Option(labels.toList),
    KillChainPhaseForm.toKillChainPhaseListOpt(kill_chain_phases),
    Option(description.value),
    Option(revoked.value),
    ExternalRefForm.toExternalRefListOpt(external_references),
    CyberConverter.toIdentifierListOpt(object_marking_refs), Option(List()),
    CyberConverter.toIdentifierOpt(created_by_ref.value), None
  )

}

object IndicatorForm {

  def clone(inForm: IndicatorForm) = {
    new IndicatorForm {
      `type`.value = inForm.`type`.value
      id.value = inForm.id.value
      name.value = inForm.name.value
      created.value = inForm.created.value
      modified.value = inForm.modified.value
      labels ++ inForm.labels
      created_by_ref.value = inForm.created_by_ref.value
      revoked.value = inForm.revoked.value
      external_references ++ inForm.external_references
      object_marking_refs ++ inForm.object_marking_refs
    }
  }

  def fromStix(stix: Indicator): IndicatorForm = new IndicatorForm {
    `type`.value = stix.`type`
    id.value = stix.id.toString()
    name.value = stix.name.getOrElse("indicator_" + CyberUtils.randDigits)
    created.value = stix.created.toString()
    modified.value = stix.modified.toString()
    labels ++= stix.labels.getOrElse(List())
    created_by_ref.value = stix.created_by_ref.getOrElse("").toString
    revoked.value = stix.revoked.getOrElse(false)
    external_references ++= ExternalRefForm.fromExternalRefList(stix.external_references.getOrElse(List()))
    object_marking_refs ++= CyberConverter.fromIdentifierList(stix.object_marking_refs.getOrElse(List()))
  }

}

class AttackPatternForm() extends CyberObj {
  `type`.value = AttackPattern.`type`
  id.value = Identifier(AttackPattern.`type`).toString()
  name.value = "attack-pattern_" + CyberUtils.randDigits

  def toStix = new AttackPattern(
    AttackPattern.`type`, Identifier.stringToIdentifier(id.value),
    Timestamp(created.value), Timestamp(modified.value),
    name.value)
}

object AttackPatternForm {

  def fromStix(stix: AttackPattern) = new AttackPatternForm {
    `type`.value = stix.`type`
    id.value = stix.id.toString()
    name.value = stix.name
    created.value = stix.created.toString()
    modified.value = stix.modified.toString()
  }

}

class KillChainPhaseForm() {
  val kill_chain_name = StringProperty("")
  val phase_name = StringProperty("")

  def toStix = new KillChainPhase(
    kill_chain_name = kill_chain_name.value,
    phase_name = phase_name.value)
}

object KillChainPhaseForm {

  def clone(inForm: KillChainPhaseForm) = {
    new KillChainPhaseForm {
      kill_chain_name.value = inForm.kill_chain_name.value
      phase_name.value = inForm.phase_name.value
    }
  }

  def fromStix(stix: KillChainPhase): KillChainPhaseForm = new KillChainPhaseForm() {
    kill_chain_name.value = stix.kill_chain_name
    phase_name.value = stix.phase_name
  }

  def toKillChainPhaseListOpt(theList: ObservableBuffer[KillChainPhaseForm]): Option[List[KillChainPhase]] = {
    if (theList == null)
      None
    else
      Option((for (s <- theList) yield s.toStix).toList)
  }

  def fromKillChainPhaseList(theList: List[KillChainPhase]): ObservableBuffer[KillChainPhaseForm] = {
    if (theList == null)
      ObservableBuffer[KillChainPhaseForm]()
    else
      (for (s <- theList) yield KillChainPhaseForm.fromStix(s)).to[ObservableBuffer]
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

  def clone(inForm: ExternalRefForm) = {
    new ExternalRefForm {
      source_name.value = inForm.source_name.value
      description.value = inForm.description.value
      url.value = inForm.url.value
      external_id.value = inForm.external_id.value
    }
  }

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

class IdentityForm() extends CyberObj {
  `type`.value = Identity.`type`
  id.value = Identifier(Identity.`type`).toString()
  name.value = "identity_" + CyberUtils.randDigits

  def toStix = new Identity(
    Identity.`type`, Identifier.stringToIdentifier(id.value),
    Timestamp(created.value), Timestamp(modified.value),
    name.value, "identity_class")
}

object IdentityForm {

  def fromStix(stix: Identity) = new IdentityForm {
    `type`.value = stix.`type`
    id.value = stix.id.toString()
    name.value = stix.name
    created.value = stix.created.toString()
    modified.value = stix.modified.toString()
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
      case stix: Indicator => IndicatorForm.fromStix(stix)
      case stix: AttackPattern => AttackPatternForm.fromStix(stix)
      case stix: Identity => IdentityForm.fromStix(stix)
      case stix: CustomStix => CustomStixForm.fromStix(stix)

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
      case _ => new IndicatorForm()
    }
  }

  def toIdentifierOpt(s: String): Option[Identifier] = {
    if (s == null || s.isEmpty) None
    else {
      val part = s.split("--")
      if (part(0).isEmpty) None
      else if (part(1).isEmpty) None
      else Option(new Identifier(part(0), part(1)))
    }
  }

  private def toIdentifier(s: String): Identifier = {
    val part = s.split("--")
    new Identifier(part(0), part(1))
  }

  def toIdentifierListOpt(theList: ObservableBuffer[String]): Option[List[Identifier]] = {
    if (theList == null)
      None
    else {
      val identifierList = mutable.ListBuffer[Identifier]()
      for (s <- theList) {
        try {
          val ident = toIdentifier(s)
          identifierList += ident
        } catch {
          case x: Throwable => println("---> incorrect identifier removed: " + s)
        }
      }
      Option(identifierList.toList)
    }
  }

  def fromIdentifierList(theList: List[Identifier]): ObservableBuffer[String] = {
    if (theList == null)
      ObservableBuffer[String]()
    else
      (for (s <- theList) yield s.toString()).to[ObservableBuffer]
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

case class ServerForm(url: StringProperty = StringProperty(""),
                      user: StringProperty = StringProperty(""),
                      psw: StringProperty = StringProperty(""))

object ServerForm {

  def clone(inForm: ServerForm) = {
    new ServerForm {
      url.value = inForm.url.value
      user.value = inForm.user.value
      psw.value = inForm.psw.value
    }
  }

}
