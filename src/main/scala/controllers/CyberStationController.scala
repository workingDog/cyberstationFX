package controllers

import javafx.fxml.FXML
import scala.concurrent.ExecutionContext.Implicits.global
import com.jfoenix.controls.{JFXSpinner, JFXTabPane}
import cyber.CyberBundle
import db.DbService
import taxii.{TaxiiCollection, TaxiiConnection}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.scene.control._
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.paint.Color
import scalafxml.core.macros.{nested, sfxml}



trait CyberStationControllerInterface {

  def init(): Unit

  def stopApp(): Unit

  def getAllBundles(): List[CyberBundle]

  def messageBar(): Label

  def messageBarSpin(): JFXSpinner

  def getSelectedServer(): StringProperty

  def getSelectedApiroot(): StringProperty

  def getSelectedCollection(): ObjectProperty[TaxiiCollection]

  def getStixViewController(): StixViewControllerInterface

  def showThis(text: String, color: Color): Unit
}

@sfxml
class CyberStationController(mainMenu: VBox,
                             loginButton: Button,
                             messageLabel: Label,
                             serversView: HBox,
                             objectsView: VBox,
                             @FXML msgBarSpinner: JFXSpinner,
                             @FXML stixView: JFXTabPane,
                             @nested[ObjectsViewController] objectsViewController: ObjectsViewControllerInterface,
                             @nested[MainMenuController] mainMenuController: MainMenuControllerInterface,
                             @nested[ServersViewController] serversViewController: ServersViewControllerInterface,
                             @nested[StixViewController] stixViewController: StixViewControllerInterface)
  extends CyberStationControllerInterface {

  override def getSelectedServer() = serversViewController.serverInfo

  override def getSelectedApiroot() = serversViewController.apirootInfo

  override def getSelectedCollection() = serversViewController.collectionInfo

  override def getStixViewController() = stixViewController

  // give this controller to the mainMenuController
  mainMenuController.setCyberStationController(this)

  // give this controller to the stixViewController
  stixViewController.setCyberStationController(this)

  // give this controller to the ObjectsViewController
  objectsViewController.setCyberStationController(this)

  override def getAllBundles() = stixViewController.getBundleController().getAllBundles()

  override def messageBar(): Label = messageLabel

  override def messageBarSpin(): JFXSpinner = msgBarSpinner

  override def init() {
    showThis("Trying to connect to database: " + DbService.dbUri, Color.Black)
    showSpinner(true)
    // try to connect to the mongo db
    Future(try {
      // start a db connection
      // will wait here for the connection to complete or throw an exception
      DbService.init()
      // load the data
      DbService.loadLocalBundles().onComplete {
        case Success(theList) =>
          showThis("Connected to database: " + DbService.dbUri, Color.Black)
          Platform.runLater(() => {
            stixViewController.getBundleController().setBundles(theList)
          })
        case Failure(err) =>
          showThis("Fail to load data from database: " + DbService.dbUri, Color.Red)
          println("---> bundles loading failure: " + err)
      }
      showSpinner(false)
    } catch {
      case ex: Throwable =>
        showThis("Fail to connect to database: " + DbService.dbUri + " --> data will not be saved", Color.Red)
        showSpinner(false)
    })
  }

  // save the data and close properly before exiting
  def saveAndStop(): Unit = {
    // todo redo this
    // delete the old bundles collection
    DbService.dropLocalBundles()
    // save the current bundles
    DbService.saveLocalBundles(getAllBundles()).onComplete {
      case Success(result) =>
        println("---> bundles saved")
        doClose()
      case Failure(err) =>
        println("---> bundles saving failure: " + err)
        doClose()
    }
  }

  override def showThis(text: String, color: Color): Unit = Platform.runLater(() => {
    messageBar().setTextFill(color)
    messageBar().setText(text)
  })

  private def showSpinner(onof: Boolean) = {
    Platform.runLater(() => {
      msgBarSpinner.setVisible(onof)
    })
  }

  private def doClose() {
    DbService.close()
    TaxiiConnection.closeSystem()
    System.exit(0)
  }

  // close properly before exiting
  override def stopApp(): Unit = {
    if (DbService.isConnected())
      saveAndStop()
    else
      doClose()
  }

}