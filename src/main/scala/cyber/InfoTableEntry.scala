package cyber

import scalafx.beans.property.StringProperty


/**
  * represents a server info entry into the scalafx UI table
  *
  */
class InfoTableEntry(val initialTitle: String, val initialInfo: String) {
  val title = StringProperty(initialTitle)
  val info = StringProperty(initialInfo)
}