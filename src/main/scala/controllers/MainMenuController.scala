package controllers

import java.io.{File, FileOutputStream}

import com.kodekutters.stix.Bundle
import cyber.CyberBundle
import play.api.libs.json.Json
import java.io.FileWriter
import java.io.IOException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scalafx.scene.control.MenuItem
import scalafx.scene.paint.Color
import scalafx.stage.{FileChooser, Stage}
import scalafxml.core.macros.sfxml


trait MainMenuControllerInterface {
  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit

  def init(): Unit

  def loadAction(): Unit

  def saveAction(): Unit

  def aboutAction(): Unit

  def newAction(): Unit

  def quitAction(): Unit
}

@sfxml
class MainMenuController(loadItem: MenuItem,
                         saveItem: MenuItem,
                         quitItem: MenuItem,
                         aboutItem: MenuItem,
                         newItem: MenuItem
                        ) extends MainMenuControllerInterface {

  var cyberController: CyberStationControllerInterface = _

  override def setCyberStationController(cyberStationController: CyberStationControllerInterface) {
    cyberController = cyberStationController
  }

  override def init() {

  }

  override def loadAction() {
    // select the bundle file to load
    Option(new FileChooser().showOpenDialog(new Stage())).map(file => loadLocalBundle(file))
  }

  override def saveAction() {
    println("---> in saveAction")
    val fileChooser = new FileChooser()
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("files", "*.json"))
    // show save file dialog
    val file = new FileChooser().showSaveDialog(new Stage())
    if (file != null) {
      println("---> in saveAction file: " + file.getName)
      // save the data to file
      // scala.tools.nsc.io.File("filename").writeAll("hello world")
//      try {
//        val theData = cyberController.getAllBundles()
//        val fileWriter = new FileWriter(file)
//        fileWriter.write(theData)
//        fileWriter.close()
//      } catch {
//        case ex: IOException =>
//      }
    }
  }

  override def aboutAction() {
    println("---> in aboutAction")
  }

  override def newAction() {

  }

  override def quitAction() {
    cyberController.stopApp()
  }

  private def loadLocalBundle(theFile: File) {
    cyberController.showThis("Loading bundle from file: " + theFile.getName, Color.Black)
    cyberController.messageBarSpin().setVisible(true)
    // try to load the data from file
    try {
      // make a bundle name from the file name
      val bundleName = theFile.getName.toLowerCase match {
        case x if x.endsWith(".json") => x.dropRight(5)
        case x if x.endsWith(".txt") => x.dropRight(4)
        case x => x
      }
      // read a bundle from theFile
      val jsondoc = Source.fromFile(theFile).mkString
      // create a bundle object from it
      Json.fromJson[Bundle](Json.parse(jsondoc)).asOpt match {
        case Some(bundle) =>
          val cyberBundle = CyberBundle.fromStix(bundle, bundleName)
          cyberController.showThis("Bundle loaded from file: " + theFile.getName, Color.Black)
          cyberController.getStixViewController().getBundleController().setBundles(List(cyberBundle))
        case None =>
          cyberController.showThis("Fail to load bundle from file: " + theFile.getName, Color.Red)
          println("---> bundle loading failure --> invalid JSON")
      }
      cyberController.messageBarSpin().setVisible(false)
    } catch {
      case ex: Throwable =>
        cyberController.showThis("Fail to load bundle from file: " + theFile.getName, Color.Red)
        cyberController.messageBarSpin().setVisible(false)
    }
  }

}
