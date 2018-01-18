package cyber

import com.kodekutters.stix._
import play.api.libs.json._

import scala.collection.mutable.ListBuffer

//-----------------------------------------------------------------------
//---------------------------supporting classes--------------------------
//-----------------------------------------------------------------------

case class CyNode(id: String, parent: Option[String] = None)

object CyNode {
  implicit val fmt = Json.format[CyNode]
}

case class CyEdge(id: String, source: String, target: String)

object CyEdge {
  implicit val fmt = Json.format[CyEdge]
}

//-----------------------------------------------------------------------
//----------------cytoscape nodes and edges------------------------------
//-----------------------------------------------------------------------

trait CytoObject

case class CytoNode(data: CyNode) extends CytoObject

object CytoNode {
  implicit val fmt = Json.format[CytoNode]
}

case class CytoEdge(data: CyEdge) extends CytoObject

object CytoEdge {
  implicit val fmt = Json.format[CytoEdge]
}

object CytoObject {

  // todo remove
  var (r, s, n, k) = (0, 0, 0, 0)

  def stixToCyto(stix: StixObj) = {
    stix match {
      case x: Relationship => r = r + 1; CytoEdge(CyEdge(x.id.toString(), x.source_ref.toString(), x.target_ref.toString()))
      case x: Sighting => s = s + 1; CytoEdge(CyEdge(x.id.toString(), x.sighting_of_ref.toString(), x.sighting_of_ref.toString()))
      case x: SDO => n = n + 1; CytoNode(CyNode(x.id.toString()))
      case x => k = k + 1; CytoNode(CyNode(x.id.toString()))
    }
  }

  def toCytoEls(stixList: ListBuffer[StixObj]): String = {
    r=0;s=0;n=0;k=0
    val cytoList = for (stix <- stixList) yield stixToCyto(stix)
    println("---> Relationship: " + r + " Sighting: " + s + " SDO: " + n + " StixObj: " + k)
    r=0;s=0;n=0;k=0
    Json.stringify(Json.toJson(cytoList.toArray))
  }

  def toCytoEls(bundle: Bundle): String = toCytoEls(bundle.objects)

  val theReads = new Reads[CytoObject] {
    def reads(js: JsValue): JsResult[CytoObject] = {
      (js \ "source").asOpt[String] match {
        case Some(x) => CytoEdge.fmt.reads(js) // if have a source it must be an edge
        case None => CytoNode.fmt.reads(js)
      }
    }
  }

  val theWrites = new Writes[CytoObject] {
    def writes(obj: CytoObject) = {
      obj match {
        case x: CytoEdge => CytoEdge.fmt.writes(x)
        case x: CytoNode => CytoNode.fmt.writes(x)
        case _ => JsNull
      }
    }
  }

  implicit val fmt: Format[CytoObject] = Format(theReads, theWrites)

}
