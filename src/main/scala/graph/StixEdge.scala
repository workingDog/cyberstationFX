package graph

import com.kodekutters.stix.{Relationship, SRO, Sighting}

//case class StixEdge(id: String, sro: SRO) {
//  override def toString: String = sro match {
//    case x: Relationship => x.relationship_type
//    case x: Sighting => x.`type`
//    case x  => ""
//  }
//
//}

case class StixEdge(id: String, sroType: String) {
  override def toString: String = sroType
}

