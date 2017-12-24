package controllers

import java.io.IOException
import javafx.fxml.FXML
import javafx.scene.text.Text

import scalafx.Includes._
import com.jfoenix.controls.{JFXButton, JFXListView, JFXSpinner}
import cyber.{CyberStationApp, InfoTableEntry, ServerForm}

import scalafx.collections.ObservableBuffer
import scalafx.scene.input.{MouseButton, MouseEvent}
import scalafxml.core.macros.sfxml
import scalafx.scene.control.TableColumn._
import scalafx.scene.control._
import com.kodekutters.taxii._
import support.CyberUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.{Modality, Stage}
import scalafxml.core.{DependenciesByType, FXMLLoader}


trait ServersViewControllerInterface {
  def init(): Unit

  val serverInfo = new ObjectProperty[ServerForm]()
  val apirootInfo = StringProperty("")
  val collectionInfo = new ObjectProperty[TaxiiCollection]()
}

@sfxml
class ServersViewController(@FXML addButton: JFXButton,
                            @FXML deleteButton: JFXButton,
                            @FXML serverSpinner: JFXSpinner,
                            @FXML serversListView: JFXListView[ServerForm],
                            @FXML collectionsListView: JFXListView[TaxiiCollection],
                            @FXML apirootsListView: JFXListView[String],
                            serverInfoTable: TableView[InfoTableEntry]) extends ServersViewControllerInterface {

  var connOpt: Option[TaxiiConnection] = None
  val serverInfoItems = ObservableBuffer[InfoTableEntry]()
  val srvList = ObservableBuffer[ServerForm](ServerForm(url = StringProperty("https://test.freetaxii.com:8000")))
  val apirootList = ObservableBuffer[String]()
  val collectionList = ObservableBuffer[TaxiiCollection]()

  init()

  // bind the serverInfo to the selected server url of the serversListView
  serverInfo <== serversListView.getSelectionModel.selectedItemProperty()
  // bind the apirootInfo to the selected apiroot of the apirootsListView
  apirootInfo <== apirootsListView.getSelectionModel.selectedItemProperty()
  // bind the collectionInfo to the selected collection of the collectionsListView
  collectionInfo <== collectionsListView.getSelectionModel.selectedItemProperty()

  def init() {
    serverSpinner.setVisible(false)
    // setup the list of servers
    serversListView.setEditable(true)
    serversListView.setExpanded(true)
    serversListView.setDepth(1)
    serversListView.setItems(srvList)
    serversListView.cellFactory = { _ =>
      new ListCell[ServerForm] {
        item.onChange { (_, _, srv) =>
          if (srv != null) text = srv.url.value
          else text = ""
        }
      }
    }
    serversListView.getSelectionModel.selectedItem.onChange { (source, oldValue, newValue) =>
      wipeInfo()
      serverSpinner.setVisible(true)
      Future {
        createServerInfo(newValue)
      }
    }
    addButton.setOnMouseClicked((_: MouseEvent) => {
      serverSpinner.setVisible(false)
      val newForm = new ServerForm() {
        url.value = "https://"
      }
      if (showServerDialog(newForm)) srvList += newForm
    })
    deleteButton.setOnMouseClicked((_: MouseEvent) => {
      val selectedItem = serversListView.getSelectionModel.getSelectedItem
      if (selectedItem != null) {
        serversListView.getSelectionModel.clearSelection()
        srvList -= selectedItem
        wipeInfo()
      }
      serverSpinner.setVisible(false)
    })
    // double click on a server entry to edit it
    serversListView.setOnMouseClicked((event: MouseEvent) => {
      if ((event.button == MouseButton.Primary) && (event.clickCount == 2) && event.getTarget.isInstanceOf[Text]) {
        showServerDialog(serversListView.getSelectionModel.getSelectedItem)
        serversListView.refresh()
      }
    })

    // setup the table of server info
    serverInfoTable.setItems(serverInfoItems)
    wipeInfo()
    serverInfoTable.editable = false
    serverInfoTable.selectionModel = null
    serverInfoTable.columns.clear()
    serverInfoTable.columns ++= List(
      new TableColumn[InfoTableEntry, String] {
        prefWidth = 100
        editable = false
        sortable = false
        cellValueFactory = _.value.title
      },
      new TableColumn[InfoTableEntry, String]() {
        prefWidth = 400
        editable = false
        sortable = false
        cellValueFactory = _.value.info
      })
    // setup the collectionsListView
    collectionsListView.setItems(collectionList)
    collectionsListView.editable = false
    collectionsListView.cellFactory = { _ =>
      new ListCell[TaxiiCollection] {
        padding = Insets(16.0, 0.0, 0.0, 0.0) // top, right, bottom, left
        item.onChange { (_, _, taxiiCol) => {
          if (taxiiCol != null) {
            val canread = if (taxiiCol.can_read) "can read" else "cannot read"
            val canwrite = if (taxiiCol.can_write) "can write to" else "cannot write to"
            val description = taxiiCol.description.getOrElse("")
            val theText = taxiiCol.title + "\n" + description + "\n" + taxiiCol.id + "\n" + "(" + canread + " - " + canwrite + ")"
            val text = new Text(theText)
            text.wrappingWidthProperty.bind(collectionsListView.widthProperty.subtract(15))
            prefWidth = 0
            graphic = text
          } else
            text = ""
        }
        }
      }
    }
    // setup the apirootsListView
    apirootsListView.setItems(apirootList)
    apirootsListView.getSelectionModel.setSelectionMode(SelectionMode.Single)
    apirootsListView.getSelectionModel.selectedItem.onChange { (_, _, newValue) =>
      collectionList.clear()
      serverSpinner.setVisible(true)
      Future {
        getCollectionsInfo(newValue)
      }
    }
  }

  def wipeInfo(): Unit = {
    apirootList.clear()
    collectionList.clear()
    serverInfoItems.clear()
    serverInfoItems += new InfoTableEntry("Title", "")
    serverInfoItems += new InfoTableEntry("Contact", "")
    serverInfoItems += new InfoTableEntry("Description", "")
    serverInfoItems += new InfoTableEntry("Default", "")
  }

  def getCollectionsInfo(apiroot: String): Unit = {
    if (apiroot == null || apiroot.isEmpty) {
      serverSpinner.setVisible(false)
      return
    }
    connOpt.map(conn => {
      // get the future response
      Collections(apiroot, conn).response onComplete {

        case Success(theResponse) =>
          serverSpinner.setVisible(false)
          theResponse match {
            case Right(taxiiCollections) =>
              collectionList.clear()
              taxiiCollections.collections.map(theList => {
                theList.foreach(taxiCol => {
                  // have to do this because we are not inside a FX-UI thread
                  Platform.runLater(() => {
                    collectionList.append(taxiCol)
                    if (collectionList.length > 0) collectionsListView.getSelectionModel.selectFirst()
                    serverSpinner.setVisible(false)
                  })
                })
              })
            case Left(taxiiErrorMessage) => showAlert(taxiiErrorMessage)
          }

        case Failure(t) => serverSpinner.setVisible(false)
      }
    })
  }

  def createServerInfo(serverForm: ServerForm) {
    // check that the url is valid
    if (serverForm.url.value == null || serverForm.url.value.isEmpty || !CyberUtils.urlValid(serverForm.url.value)) {
      serverSpinner.setVisible(false)
      return
    }
    // close any previous connection
    connOpt.map(conn => conn.close())
    // create a new connection object
    connOpt = Some(new TaxiiConnection(serverForm.url.value, serverForm.user.value, serverForm.psw.value, 5))
    val server = Server(conn = connOpt.get)
    // get the future response from the server
    server.response onComplete {

      case Success(theResponse) =>
        serverSpinner.setVisible(false)
        theResponse match {
          case Right(d) =>
            // the discovery
            serverInfoItems.clear()
            serverInfoItems += new InfoTableEntry("Title", d.title)
            serverInfoItems += new InfoTableEntry("Contact", d.contact.getOrElse(""))
            serverInfoItems += new InfoTableEntry("Description", d.description.getOrElse(""))
            serverInfoItems += new InfoTableEntry("Default", d.default.getOrElse(""))
            // the api roots
            val theList = d.api_roots.map(rootList =>
              for (apiRoot <- rootList) yield ApiRoot(apiRoot, connOpt.get).api_root).getOrElse(List.empty)
            apirootList.clear()
            // have to do this because we are inside a FX-UI thread
            Platform.runLater(() => {
              theList.foreach(s => apirootList.append(s))
              if (apirootList.length > 0) apirootsListView.getSelectionModel.selectFirst()
            })

          case Left(taxiiErrorMessage) => showAlert(taxiiErrorMessage)
        }

      case Failure(t) => serverSpinner.setVisible(false)
    }
  }

  def showAlert(taxiiErrorMessage: TaxiiErrorMessage) = {
    Platform.runLater(() => {
      new Alert(AlertType.Warning) {
        initOwner(this.owner)
        title = "Connection problem"
        headerText = taxiiErrorMessage.title
        contentText = taxiiErrorMessage.description.getOrElse("")
      }.showAndWait()
      Platform.implicitExit = true
    })
  }


  // popup the server info editor dialog
  def showServerDialog(serverForm: ServerForm): Boolean =
    try {
      // record the initial values, in case we cancel
      val formCopy = ServerForm.clone(serverForm)
      // load the fxml file
      val resource = CyberStationApp.getClass.getResource("forms/serverDialog.fxml")
      if (resource == null) {
        throw new IOException("Cannot load resource: forms/serverDialog.fxml")
      }
      val loader = new FXMLLoader(resource, new DependenciesByType(Map.empty))
      val pane = loader.load.asInstanceOf[javafx.scene.layout.GridPane]
      val scene = new Scene(pane)
      // create the dialog Stage
      val theStage = new Stage()
      theStage.setTitle("Server info")
      theStage.initModality(Modality.WindowModal)
      theStage.initOwner(CyberStationApp.stage)
      theStage.setScene(scene)
      // give the stage and server info to the controller
      val controller = loader.getController[ServerDialogControllerInterface]()
      controller.setDialogStage(theStage)
      controller.setServerInfo(serverForm)
      // show the dialog and wait until the user closes it
      theStage.showAndWait
      // if cancel, reset to the previous values
      if (!controller.isOkClicked()) {
        serverForm.url.value = formCopy.url.value
        serverForm.user.value = formCopy.user.value
        serverForm.psw.value = formCopy.psw.value
      }
      // return true if the ok button was clicked else false
      controller.isOkClicked()
    } catch {
      case e: IOException =>
        e.printStackTrace()
        false
    }

}
