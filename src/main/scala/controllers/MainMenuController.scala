package controllers

import java.io.File

import com.kodekutters.stix.Bundle
import cyber.CyberBundle
import play.api.libs.json.Json
import java.io.IOException
import java.nio.file.{Files, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}

import util.CyberUtils

import scala.io.Source
import scala.language.implicitConversions
import scala.language.postfixOps

import scalafx.scene.control.MenuItem
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser.ExtensionFilter
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
    val file = new FileChooser {
      title = "Save bundle file"
      extensionFilters.add(new ExtensionFilter("zip", "*.zip"))
    }.showSaveDialog(new Stage())
    if (file != null) {
      // create the zip file
      val zip = new ZipOutputStream(Files.newOutputStream(Paths.get(file.getPath)))
      // for each bundle of stix
      cyberController.getAllBundles().foreach { bundle =>
        val fileName = if ((bundle.name.value == null) || bundle.name.value.isEmpty)
          CyberUtils.randName + ".json"
        else
          bundle.name.value + ".json"
        zip.putNextEntry(new ZipEntry(fileName))
        try {
          zip.write(Json.stringify(Json.toJson(bundle.toStix)).getBytes)
        } catch {
          case e: IOException => e.printStackTrace()
        }
        finally {
          zip.closeEntry()
        }
      }
      zip.close()
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
