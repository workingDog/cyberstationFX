package graph

import com.kodekutters.stix.SDO

//case class StixNode(id: String, sdo: SDO) {
//  override def toString: String = sdo.`type`
//}

case class StixNode(id: String, sdoType: String) {
  override def toString: String = sdoType
}

