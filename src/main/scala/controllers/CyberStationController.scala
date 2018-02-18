package controllers


import javafx.fxml.FXML

import scala.concurrent.ExecutionContext.Implicits.global
import com.jfoenix.controls.{JFXSpinner, JFXTabPane}
import com.kodekutters.stix.StixObj
import cyber.{CyberBundle, ServerForm}
import db.DbService
import com.kodekutters.taxii.{TaxiiCollection, TaxiiConnection}
import com.typesafe.config.{Config, ConfigFactory}
import db.mongo.MongoDbStix

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.event.Event
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.paint.Color
import scalafxml.core.macros.{nested, sfxml}
import scalafx.Includes._
import scala.collection.mutable.ListBuffer
import scalafx.scene.web.WebView


trait CyberStationControllerInterface {

  def init(): Unit

  def stopApp(): Unit

  def doClose(): Unit

  def confirmAndSave(): Unit

  def getAllBundles(): ObservableBuffer[CyberBundle]

  def messageBar(): Label

  def messageBarSpin(): JFXSpinner

  def getSelectedServer(): ObjectProperty[ServerForm]

  def getSelectedApiroot(): StringProperty

  def getSelectedCollection(): ObjectProperty[TaxiiCollection]

  def getStixViewController(): StixViewControllerInterface

  def showThis(text: String, color: Color): Unit

  def showSpinner(onof: Boolean): Unit

  def onChangeAction(e: Event): Unit

  def viewTestBundle(stixList: ListBuffer[StixObj], theText: String): Unit

}

@sfxml
class CyberStationController(mainMenu: VBox,
                             messageLabel: Label,
                             serversView: HBox,
                             objectsView: VBox,
                             neo4jweb: WebView,
                             @FXML msgBarSpinner: JFXSpinner,
                             @FXML stixView: JFXTabPane,
                             @nested[MainMenuController] mainMenuController: MainMenuControllerInterface,
                             @nested[ServersViewController] serversViewController: ServersViewControllerInterface,
                             @nested[WebViewController] webViewController: WebViewControllerInterface,
                             @nested[StixViewController] stixViewController: StixViewControllerInterface)
  extends CyberStationControllerInterface {

  val config: Config = ConfigFactory.load

  var theTempPath: String = _

  def getSelectedServer() = serversViewController.serverInfo

  def getSelectedApiroot() = serversViewController.apirootInfo

  def getSelectedCollection() = serversViewController.collectionInfo

  def getStixViewController() = stixViewController

  // give this controller to the mainMenuController
  mainMenuController.setCyberStationController(this)

  // give this controller to the stixViewController
  stixViewController.setCyberStationController(this)

  // give this controller to the webViewController
  webViewController.setCyberStationController(this)

  // give this controller to the graphViewController
  //  graphViewController.setCyberStationController(this)

  def getAllBundles() = stixViewController.getBundleController().getAllBundles()

  def messageBar(): Label = messageLabel

  def messageBarSpin(): JFXSpinner = msgBarSpinner

  def initToolMongo(): Unit = {
    showThis("Trying to connect to database: " + MongoDbStix.getUri(), Color.Black)
    showSpinner(true)
    // try to connect to the mongo db
    Future(try {
      // start a mongo DB connection, for the save to file tool
      // will wait here for the connection to complete or throw an exception
      MongoDbStix.init()
    } catch {
      case ex: Throwable =>
        showThis("Fail to connect to database: " + MongoDbStix.getUri() + " --> data will not be saved to the database", Color.Red)
    } finally {
      showSpinner(false)
    })
  }

  def initLocalDB(): Unit = {
    showThis("Trying to connect to database: " + DbService.getUri(), Color.Black)
    showSpinner(true)
    // try to connect to the mongo db
    Future(try {
      // start a db connection, for local bundle storage
      // will wait here for the connection to complete or throw an exception
      DbService.init()
      // load the data
      DbService.loadLocalBundles().onComplete {
        case Success(theList) =>
          showThis("Connected to database: " + DbService.getUri(), Color.Black)
          Platform.runLater(() => stixViewController.getBundleController().setBundles(theList))
        case Failure(err) =>
          showThis("Fail to load data from database: " + DbService.getUri(), Color.Red)
          println("---> bundles loading failure: " + err)
      }
    } catch {
      case ex: Throwable =>
        showThis("Fail to connect to database: " + DbService.getUri() + " --> data will not be saved to the database", Color.Red)
    } finally {
      showSpinner(false)
    })
  }

  def init(): Unit = {
    showSpinner(false)
    // try to connect to the mongo db, for the save to file tool
    initToolMongo()
    // try to connect to the mongo db, for local storage
    initLocalDB()
  }

  // save the data and close properly before exiting
  def saveAndStop(): Unit = {
    // todo redo this
    // delete the old bundles collection
    DbService.dropLocalBundles()
    // save the current bundles
    DbService.saveLocalBundles(getAllBundles().toList).onComplete {
      case Success(result) =>
        println("---> bundles saved")
        doClose()
      case Failure(err) =>
        println("---> bundles saving failure: " + err)
        doClose()
    }
  }

  def showThis(text: String, color: Color): Unit = Platform.runLater(() => {
    messageBar().setTextFill(color)
    messageBar().setText(text)
  })

  def showSpinner(onof: Boolean) = Platform.runLater(() => msgBarSpinner.setVisible(onof))

  def doClose(): Unit = {
    DbService.close()
    TaxiiConnection.closeSystem()
    System.exit(0)
  }

  // close properly before exiting
  override def stopApp(): Unit = if (getAllBundles().toList.nonEmpty) confirmAndSave() else doClose()

  def confirmAndSave(): Unit = {
    val ButtonTypeYes = new ButtonType("Yes")
    val ButtonTypeNo = new ButtonType("No")
    val alert = new Alert(AlertType.Confirmation) {
      initOwner(this.owner)
      title = "About to exit CyberStation"
      headerText = "Save current bundles data before exiting"
      contentText = "Confirm saving bundles"
      buttonTypes = Seq(ButtonTypeYes, ButtonTypeNo)
    }
    val result = alert.showAndWait()
    result match {
      case Some(ButtonTypeYes) =>
        if (DbService.isConnected())
          saveAndStop()
        else
          doClose()
      case _ => doClose()
    }
  }

  /**
    * the action when the "stixViewTab" is selected
    */
  def onChangeAction(e: Event) = {
    if (e.source.isInstanceOf[javafx.scene.control.Tab]) {
      val source = e.source.asInstanceOf[javafx.scene.control.Tab]
      if (source.id.value == "stixViewTab" && source.selected.value) {
        val selected = webViewController.getButonGroup().selectedToggle.value
        if (selected != null) {
          selected.setSelected(!selected.isSelected)
        }
        webViewController.doClear()
      }
    }
  }

  def viewTestBundle(stixList: ListBuffer[StixObj], theText: String): Unit = {
    webViewController.doLoad(stixList, theText)
    //  graphViewController.doLoadAndClick(stixList, theText)
  }

}
