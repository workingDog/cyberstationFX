package controllers

import java.io.File

import com.kodekutters.stix.Bundle
import cyber.{CyberBundle, CyberStationApp, FileSender}
import play.api.libs.json.Json
import java.io.IOException
import java.nio.file.{Files, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}

import db._
import db.mongo.{MongoLocalService, MongoDbStix}
import db.neo4j.Neo4jService

import scala.concurrent.ExecutionContext.Implicits.global
import support.CyberUtils

import scala.collection.mutable
import scala.io.Source
import scala.language.implicitConversions
import scala.language.postfixOps
import scalafx.scene.control.{Alert, ButtonType, MenuItem}
import scalafx.scene.paint.Color
import scalafxml.core.macros.sfxml
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}
import scalafx.application.Platform
import scalafx.scene.control.Alert.AlertType
import CyberUtils._

import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.{FileChooser, Stage}

trait MainMenuControllerInterface {
  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit

  def loadAction(): Unit

  def sendFromFile(): Unit

  def saveAction(): Unit

  def aboutAction(): Unit

  def newAction(): Unit

  def quitAction(): Unit

  def saveToNeo4jDB(): Unit

  def saveToMongoDB(): Unit
}

@sfxml
class MainMenuController(loadItem: MenuItem,
                         sendFromFileItem: MenuItem,
                         saveToNeo4jItem: MenuItem,
                         saveToMongoItem: MenuItem,
                         saveItem: MenuItem,
                         quitItem: MenuItem,
                         aboutItem: MenuItem,
                         testItem: MenuItem,
                         newItem: MenuItem) extends MainMenuControllerInterface {

  var cyberController: CyberStationControllerInterface = _


  override def setCyberStationController(cyberStationController: CyberStationControllerInterface) {
    cyberController = cyberStationController
  }

  /**
    * load bundles from a file to the viewer
    */
  override def loadAction() {
    fileSelector().map(file => loadLocalBundles(file))
  }

  /**
    * load bundles from a file and sent it to the server
    */
  override def sendFromFile() {
    fileSelector().map(file => FileSender.sendFile(file, cyberController))
  }

  /**
    * save the bundles to a zip file  ("zip", "*.zip")
    */
  override def saveAction() {
    if (cyberController.getAllBundles().toList.nonEmpty) {
      val file = new FileChooser {
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
  }

  override def aboutAction() {
    new Alert(AlertType.Information) {
      initOwner(this.owner)
      title = "CyberStation-" + CyberStationApp.version
      headerText = None
      contentText = "CyberStation is a tool to create and send STIX-2 objects to TAXII-2 servers."
    }.showAndWait()
  }

  override def newAction() {
    if (cyberController.getAllBundles().toList.nonEmpty) {
      val ButtonTypeYes = new ButtonType("Yes")
      val ButtonTypeNo = new ButtonType("No")
      val alert = new Alert(AlertType.Confirmation) {
        initOwner(this.owner)
        title = "About to clear the current bundles data"
        headerText = "Save current bundles data before clearing"
        contentText = "Confirm saving bundles"
        buttonTypes = Seq(ButtonTypeYes, ButtonTypeNo)
      }
      val result = alert.showAndWait()
      result match {
        case Some(ButtonTypeYes) =>
          // todo redo this
          // delete the old bundles collection
          DbService.dropLocalBundles()
          // save the current bundles
          DbService.saveLocalBundles(cyberController.getAllBundles().toList).onComplete {
            case Success(result) => println("---> bundles saved")
            case Failure(err) => println("---> bundles saving failure: " + err)
          }
        case _ =>
      }
      // clear bundles
      Platform.runLater(() => {
        cyberController.getStixViewController().getBundleController().getAllBundles().clear()
        cyberController.getStixViewController().getBundleController().getBundleStixView().getItems.clear()
      })
    }
  }

  /**
    * stop all processes and exit from the app
    */
  override def quitAction() {
    if (cyberController.getAllBundles().toList.nonEmpty) cyberController.confirmAndSave()
    else cyberController.doClose()
  }

  private def showSpinner(onof: Boolean) = {
    Platform.runLater(() => {
      cyberController.messageBarSpin().setVisible(onof)
    })
  }

  /**
    * read a zip file containing bundles of stix
    *
    */
  private def loadLocalZipBundles(theFile: File) {
    cyberController.showThis("Loading bundles from file: " + theFile.getName, Color.Black)
    showSpinner(true)
    // try to load the data from a zip file
    try {
      val rootZip = new java.util.zip.ZipFile(theFile)
      val bundleList = mutable.ListBuffer[CyberBundle]()
      rootZip.entries.asScala.foreach(stixFile => {
        // read a STIX bundle from the InputStream
        val jsondoc = Source.fromInputStream(rootZip.getInputStream(stixFile)).mkString
        val bundleName = stixFile.getName.toLowerCase match {
          case x if x.endsWith(".json") => x.dropRight(5)
          case x if x.endsWith(".stix") => x.dropRight(5)
          case x if x.endsWith(".txt") => x.dropRight(4)
          case x => x
        }
        // create a bundle object
        Json.fromJson[Bundle](Json.parse(jsondoc)).asOpt match {
          case Some(bundle) => bundleList += CyberBundle.fromStix(bundle, bundleName)
          case None =>
            cyberController.showThis("Fail to load bundle from file: " + stixFile.getName, Color.Red)
            println("---> bundle loading failure, invalid json in: " + stixFile.getName)
        }
      })
      Platform.runLater(() => {
        cyberController.getStixViewController().getBundleController().setBundles(bundleList.toList)
      })
      cyberController.showThis("", Color.Black)
    } catch {
      case ex: Throwable =>
        println("---> Fail to load bundles from file: " + theFile.getName)
        cyberController.showThis("Fail to load bundles from file: " + theFile.getName, Color.Red)
    } finally {
      showSpinner(false)
    }
  }

  private def loadLocalBundles(theFile: File) {
    if (!theFile.getName.toLowerCase.endsWith(".zip"))
      loadLocalBundle(theFile)
    else
      loadLocalZipBundles(theFile)
  }

  /**
    * load one bundle from a json, stix or text file
    *
    * @param theFile
    */
  private def loadLocalBundle(theFile: File) {
    cyberController.showThis("Loading bundle from file: " + theFile.getName, Color.Black)
    showSpinner(true)
    // try to load the data from file
    try {
      // make a bundle name from the file name
      val bundleName = theFile.getName.toLowerCase match {
        case x if x.endsWith(".json") => x.dropRight(5)
        case x if x.endsWith(".stix") => x.dropRight(5)
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
    } catch {
      case ex: Throwable =>
        cyberController.showThis("Fail to load bundle from file: " + theFile.getName, Color.Red)
    } finally {
      showSpinner(false)
    }
  }

  /**
    * save a file to a Neo4jDB
    */
  override def saveToNeo4jDB() {
    fileSelector().map(file => Neo4jService.saveFileToDB(file, cyberController))
  }

  /**
    * save a file to a MongoDB
    */
  override def saveToMongoDB() {
    fileSelector().map(file => MongoDbStix.saveFileToDB(file, cyberController))
  }

  def testAction() {
    MongoDbStix.saveMongoToNeo4j(cyberController)
  }


}
