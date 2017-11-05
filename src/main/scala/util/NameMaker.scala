package util

import cyberProtocol.CyberObj


// Indicator, ObservedData, Relationship, Sighting, LanguageContent, Bundle

object NameMaker {

  def from(obj: CyberObj): String = {
    if (obj == null) "" else obj.name.value + " (" + obj.id.value + ")"
  }

}


