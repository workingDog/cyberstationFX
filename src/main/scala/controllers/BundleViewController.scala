package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXListView, JFXSpinner, JFXTextField}
import com.kodekutters.stix.{Bundle, Identifier}
import cyber.{CyberBundle, CyberObj, InfoTableEntry, ServerForm}
import db.DbService
import com.kodekutters.taxii.{Collection, TaxiiCollection, TaxiiConnection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.{Label, ListCell, TableColumn, TableView}
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color
import scalafx.util.StringConverter
import scalafxml.core.macros.sfxml


trait BundleViewControllerInterface {
  def init(): Unit

  def addStixToBundle(stix: CyberObj)

  def removeStixFromBundle(stix: CyberObj)

  def getCurrentBundle(): ReadOnlyObjectProperty[CyberBundle]

  def getBundleStixView(): JFXListView[CyberObj]

  def getAllBundles(): ObservableBuffer[CyberBundle]

  def setBundles(theBundles: List[CyberBundle])

  def setCyberStationController(cyberStationController: CyberStationControllerInterface)
}

@sfxml
class BundleViewController(bundleViewBox: VBox,
                           @FXML idButton: JFXButton,
                           @FXML sendButton: JFXButton,
                           @FXML addButton: JFXButton,
                           @FXML deleteButton: JFXButton,
                           @FXML serverSpinner: JFXSpinner,
                           @FXML bundleId: JFXTextField,
                           @FXML bundleVersion: JFXTextField,
                           @FXML bundleStixView: JFXListView[CyberObj],
                           @FXML bundlesListView: JFXListView[CyberBundle],
                           connectionInfo: TableView[InfoTableEntry],
                           bundleName: Label) extends BundleViewControllerInterface {

  val bundleList = ObservableBuffer[CyberBundle]()
  bundleList.onChange((source, changes) => {
    if (bundleList.isEmpty) sendButton.setDisable(true)
  })

  var serverInfo: ServerForm = _
  val connInfo = ObservableBuffer[InfoTableEntry]()
  var taxiiApiroot: Option[String] = None
  var taxiiCol: Option[TaxiiCollection] = None
  var cyberStationController: CyberStationControllerInterface = null

  init()

  def getCurrentBundle() = bundlesListView.getSelectionModel.selectedItemProperty()

  def getBundleStixView() = bundleStixView

  def addStixToBundle(stix: CyberObj) {
    if (bundlesListView.getSelectionModel.getSelectedItem != null)
      bundlesListView.getSelectionModel.getSelectedItem.objects += stix
  }

  def removeStixFromBundle(stix: CyberObj) {
    if (bundlesListView.getSelectionModel.getSelectedItem != null)
      bundlesListView.getSelectionModel.getSelectedItem.objects -= stix
  }

  def init() {
    serverSpinner.setVisible(false)
    // setup the list of bundles (showing names)
    bundlesListView.setEditable(true)
    bundlesListView.setExpanded(true)
    bundlesListView.setDepth(1)
    bundlesListView.setItems(bundleList)
    bundlesListView.cellFactory = { _ =>
      new TextFieldListCell[CyberBundle] {
        converter = cyberStringConverter
        item.onChange { (_, _, bndl) => if (bndl != null) text = bndl.name.value }
      }
    }
    bundlesListView.getSelectionModel.selectedItem.onChange { (_, _, newValue) =>
      if (newValue != null) {
        bundleStixView.setItems(bundlesListView.getSelectionModel.getSelectedItem.objects)
        bundleName.text <== newValue.name
        bundleId.text = newValue.id.value
        bundleVersion.textProperty <== newValue.spec_version
      } else {
        bundleName.text.unbind()
        bundleName.text = ""
        bundleId.text = ""
        bundleVersion.text.unbind()
        bundleVersion.text = ""
      }
    }
    // todo remove this
    //    if (bundleList.isEmpty) {
    //      bundleList += new CyberBundle()
    //      bundlesListView.getSelectionModel.selectFirst()
    //    }

    // setup the table of connection info
    wipeConnInfo()
    connectionInfo.setItems(connInfo)
    connectionInfo.editable = false
    connectionInfo.selectionModel = null
    connectionInfo.tableMenuButtonVisible = false
    connectionInfo.columns.clear()
    connectionInfo.columns ++= List(
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
    // setup the bundle content of stix objects
    bundleStixView.setEditable(false)
    bundleStixView.setExpanded(true)
    bundleStixView.setDepth(1)
    bundleStixView.cellFactory = { _ =>
      new ListCell[CyberObj] {
        item.onChange { (_, _, stix) => text = makeNameFrom(stix) }
      }
    }
    // start with a disable sendButton
    //  sendButton.setDisable(true)
    sendButton.setOnMouseClicked((_: MouseEvent) => {
      serverSpinner.setVisible(true)
      Future {
        sendToServer()
      }
    })
  }

  def sendToServer(): Unit = {
    val theBundle = bundlesListView.getSelectionModel.getSelectedItem
    if (taxiiCol.isEmpty || taxiiApiroot.isEmpty || theBundle == null) {
      serverSpinner.setVisible(false)
    } else {
      taxiiCol.map(colInfo => {
        taxiiApiroot.map(apiroot => {
          val col = Collection(colInfo, apiroot, new TaxiiConnection(serverInfo.url.value,
              serverInfo.user.value, serverInfo.psw.value, 10))
          col.addObjects(theBundle.toStix).map(status => {
            println("----> status: " + status.getOrElse(theBundle.name.value + " could not be sent to the server"))
            showThis("status: " + status.getOrElse(theBundle.name.value + " could not be sent to the server"), Color.Red)
            col.conn.close()
            serverSpinner.setVisible(false)
          })
          // save the bundle of stix and the user log to the db
          DbService.saveServerBundle(theBundle.toStix, col.basePath)
          // show message on messageBar
          showThis(theBundle.name.value + " sent to the server", Color.Black)
        })
      })
    }
  }

  def wipeConnInfo(): Unit = {
    connInfo.clear()
    connInfo.add(0, new InfoTableEntry("Server", ""))
    connInfo.add(1, new InfoTableEntry("Api root", ""))
    connInfo.add(2, new InfoTableEntry("Collection", ""))
  }

  idButton.setOnMouseClicked((_: MouseEvent) => {
    if (bundlesListView.getSelectionModel.getSelectedItem != null) {
      bundleId.textProperty.unbind()
      bundleId.text = Identifier(Bundle.`type`).toString()
    }
  })

  addButton.setOnMouseClicked((_: MouseEvent) => {
    val newBundle = new CyberBundle()
    bundleList.append(newBundle)
    bundlesListView.getSelectionModel.select(newBundle)
  })

  deleteButton.setOnMouseClicked((_: MouseEvent) => {
    val selectedItem = bundlesListView.getSelectionModel.getSelectedItem
    if (selectedItem != null) {
      val ndx = bundleList.indexWhere(b => b.id == selectedItem.id)
      if (ndx != -1) bundleList.remove(ndx)
      bundlesListView.getSelectionModel.selectFirst()
    }
  })

  // for use in bundlesListView, to allow editing (double click) of the list entries text, the bundle name
  val cyberStringConverter = new StringConverter[CyberBundle] {

    def fromString(newName: String): CyberBundle = {
      bundlesListView.getSelectionModel.getSelectedItem.name.value = newName
      bundlesListView.getSelectionModel.getSelectedItem
    }

    def toString(bndl: CyberBundle): String = bndl.name.value
  }

  def makeNameFrom(obj: CyberObj): String = {
    if (obj == null) "" else obj.name.value + " (" + obj.id.value + ")"
  }

  def getAllBundles(): ObservableBuffer[CyberBundle] = bundleList

  def setBundles(theBundles: List[CyberBundle]): Unit = bundleList ++= theBundles

  def setCyberStationController(cyberController: CyberStationControllerInterface): Unit = {
    cyberStationController = cyberController

    cyberStationController.getSelectedServer().onChange { (_, oldValue, newValue) =>
      if (newValue != null) {
        connInfo.update(0, new InfoTableEntry("Server", newValue.url.value))
        serverInfo = newValue
      }
    }

    cyberStationController.getSelectedApiroot().onChange { (_, oldValue, newValue) => {
      if (newValue != null) {
        taxiiApiroot = Option(newValue)
        connInfo.update(1, new InfoTableEntry("Api root", newValue))
      } else {
        taxiiApiroot = None
      }
    }
    }

    cyberStationController.getSelectedCollection().onChange { (_, oldValue, newValue) =>
      if (newValue != null) {
        taxiiCol = Option(newValue)
        val canWrite = if (newValue.can_write) "\n(can write to)" else "\n(cannot write to)"
        sendButton.setDisable(!newValue.can_write)
        connInfo.update(2, new InfoTableEntry("Collection", newValue.title + canWrite))
      }
      else {
        taxiiCol = None
        sendButton.setDisable(true)
        connInfo.update(2, new InfoTableEntry("Collection", ""))
      }
    }
  }

  private def showThis(text: String, color: Color) = Platform.runLater(() => {
    if (cyberStationController != null) {
      cyberStationController.messageBar().setTextFill(color)
      cyberStationController.messageBar().setText(text)
    }
  })

}

