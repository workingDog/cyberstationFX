package controllers

import javafx.fxml.FXML

import scalafx.Includes._
import com.jfoenix.controls.{JFXButton, JFXListView, JFXSpinner}
import cyber.InfoTableEntry

import scalafx.collections.ObservableBuffer
import scalafx.scene.input.MouseEvent
import scalafxml.core.macros.sfxml
import scalafx.scene.control.TableColumn._
import scalafx.scene.control._
import taxii._
import util.Utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.cell.TextFieldListCell


trait ServersViewControllerInterface {
  def init(): Unit

  val serverInfo = StringProperty("")
  val apirootInfo = StringProperty("")
  val collectionInfo = StringProperty("")
}

@sfxml
class ServersViewController(@FXML addButton: JFXButton,
                            @FXML deleteButton: JFXButton,
                            @FXML serverSpinner: JFXSpinner,
                            @FXML serversListView: JFXListView[String],
                            @FXML collectionsListView: JFXListView[String],
                            @FXML apirootsListView: JFXListView[String],
                            serverInfoTable: TableView[InfoTableEntry]) extends ServersViewControllerInterface {

  var connOpt: Option[TaxiiConnection] = None
  val serverInfoItems = ObservableBuffer[InfoTableEntry]()
  val srvList = ObservableBuffer[String](
    "https://test.freetaxii.com:8000",
    "https://test.freetaxii.com:8001")
  val apirootList = ObservableBuffer[String]()
  val collectionList = ObservableBuffer[String]()
  var collection: Option[String] = None
  val taxiiColInfo = new ObjectProperty[TaxiiCollection]()

  init()

  // bind the serverInfo to the selected server of the serversListView
  serverInfo <== serversListView.getSelectionModel.selectedItemProperty()
  // bind the apirootInfo to the selected apiroot of the apirootsListView
  apirootInfo <== apirootsListView.getSelectionModel.selectedItemProperty()
  // bind the collectionInfo to the selected collection of the collectionsListView
  collectionInfo <== collectionsListView.getSelectionModel.selectedItemProperty()

  override def init() {
    serverSpinner.setVisible(false)
    // setup the list of server url
    serversListView.setEditable(true)
    serversListView.setExpanded(true)
    serversListView.setDepth(1)
    serversListView.setItems(srvList)
    serversListView.cellFactory = TextFieldListCell.forListView()
    serversListView.getSelectionModel.selectedItem.onChange { (source, oldValue, newValue) =>
      wipeInfo()
      serverSpinner.setVisible(true)
      Future {
        getServerInfo(newValue)
      }
    }
    // setup the table of server info
    serverInfoTable.setItems(serverInfoItems)
    wipeInfo()
    serverInfoTable.editable = false
    serverInfoTable.selectionModel = null
    serverInfoTable.tableMenuButtonVisible = false
    serverInfoTable.columns.clear()
    serverInfoTable.columns ++= List(
      new TableColumn[InfoTableEntry, String] {
        prefWidth = 100
        editable = false
        sortable = false
        cellValueFactory = {
          _.value.title
        }
      },
      new TableColumn[InfoTableEntry, String]() {
        prefWidth = 400
        editable = false
        sortable = false
        cellValueFactory = {
          _.value.info
        }
      })
    // setup the collectionsListView
    collectionsListView.setItems(collectionList)
    collectionsListView.getSelectionModel.selectedItem.onChange { (_, _, newValue) =>
      collection = Some(newValue)
    }
    // setup the apirootsListView
    apirootsListView.setItems(apirootList)
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

  def getCollectionsInfo(apirootShort: String): Unit = {
    if (apirootShort == null || apirootShort.isEmpty) {
      serverSpinner.setVisible(false)
      return
    }
    connOpt.map(conn => {
      val cols = Collections(conn.baseURL + apirootShort, conn)
      cols.collections().map(theList => {
        theList.foreach(col => {
          val canread = if (col.taxiiCollection.can_read) "can read" else "cannot read"
          val canwrite = if (col.taxiiCollection.can_write) "can write to" else "cannot write to"
          val description = col.taxiiCollection.description.getOrElse("")
          val info = col.taxiiCollection.title + "\n" + description + "\n" + col.taxiiCollection.id + "\n" + "(" + canread + " - " + canwrite + ")"
          // have to do this because we are inside a FX-UI thread
          Platform.runLater(() => {
            collectionList.append(info)
            if (collectionList.length > 0) collectionsListView.getSelectionModel.selectFirst()
          })
        })
        serverSpinner.setVisible(false)
      })
    })
  }

  def getServerInfo(url: String) {
    // check that the url is valid
    if (url == null || url.isEmpty || !Utils.urlValid(url)) {
      serverSpinner.setVisible(false)
      return
    }
    connOpt = Some(new TaxiiConnection(url, "user", "psw", 5))
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
              theList.foreach(s => apirootList.append(s.replace(server.conn.baseURL, "")))
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

  addButton.setOnMouseClicked((_: MouseEvent) => {
    serverSpinner.setVisible(false)
    srvList.append("https://")
  })

  deleteButton.setOnMouseClicked((_: MouseEvent) => {
    val selectedItem = serversListView.getSelectionModel.getSelectedItem
    if (selectedItem != null) {
      serversListView.getSelectionModel.clearSelection()
      srvList.remove(srvList.indexOf(selectedItem), 1)
      wipeInfo()
    }
    serverSpinner.setVisible(false)
  })

}
