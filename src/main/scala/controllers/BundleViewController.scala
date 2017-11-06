package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXListView, JFXSpinner, JFXTextField}
import com.kodekutters.stix.{Bundle, Identifier}
import cyber.InfoTableEntry
import cyberProtocol.{CyberBundle, CyberObj}
import taxii.{Collection, Server}
import util.NameMaker

import scala.language.postfixOps
import scalafx.Includes._
import scalafx.beans.property.{ReadOnlyObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.{Label, ListCell, TableColumn, TableView}
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.VBox
import scalafx.util.StringConverter
import scalafxml.core.macros.sfxml


trait BundleViewControllerInterface {
  def init(): Unit
  def setSelectedServer(theSelectedServer: StringProperty): Unit
  def setSelectedApiroot(theSelectedApiroot: StringProperty): Unit
  def setSelectedCollection(theSelectedCollection: StringProperty): Unit
  def addStixToBundle(stix: CyberObj)
  def removeStixFromBundle(stix: CyberObj)
  def getCurrentBundle(): ReadOnlyObjectProperty[CyberBundle]
  def getBundleStixView: JFXListView[CyberObj]
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
  val connInfo = ObservableBuffer[InfoTableEntry]()
  var theServer: Option[Server] = None
  var theCollection: Option[Collection] = None

  init()

  def getCurrentBundle() = bundlesListView.getSelectionModel.selectedItemProperty()

  def getBundleStixView() = bundleStixView


  override def addStixToBundle(stix: CyberObj) {
    if (bundlesListView.getSelectionModel.getSelectedItem != null)
      bundlesListView.getSelectionModel.getSelectedItem.objects += stix
  }

  override def removeStixFromBundle(stix: CyberObj) {
    if (bundlesListView.getSelectionModel.getSelectedItem != null)
      bundlesListView.getSelectionModel.getSelectedItem.objects -= stix
  }

  override def setSelectedServer(theSelectedServer: StringProperty) {
    theSelectedServer.onChange { (_, oldValue, newValue) =>
      connInfo.update(0, new InfoTableEntry("Server", newValue))
    }
  }

  override def setSelectedApiroot(theSelectedApiroot: StringProperty) {
    theSelectedApiroot.onChange { (_, oldValue, newValue) =>
      connInfo.update(1, new InfoTableEntry("Api root", newValue))
    }
  }

  override def setSelectedCollection(theSelectedCollection: StringProperty) {
    theSelectedCollection.onChange { (_, oldValue, newValue) =>
      connInfo.update(2, new InfoTableEntry("Collection", newValue))
    }
  }

  override def init() {
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
    // setup the table of connection info
    connectionInfo.setItems(connInfo)
    wipeConnInfo()
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
        item.onChange { (_, _, stix) => text = NameMaker.from(stix) }
      }
    }
  }

  def wipeConnInfo(): Unit = {
    connInfo.clear()
    connInfo.add(0, new InfoTableEntry("Server", ""))
    connInfo.add(1, new InfoTableEntry("Api root", ""))
    connInfo.add(2, new InfoTableEntry("Collection", ""))
  }

  idButton.setOnMouseClicked((_: MouseEvent) => {
    if(bundlesListView.getSelectionModel.getSelectedItem != null) {
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

}

