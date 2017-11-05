package controllers

import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait MainMenuControllerInterface {
  def init(): Unit

}

@sfxml
class MainMenuController(theLabel: Label) extends MainMenuControllerInterface {


  override def init() {

  }

}
