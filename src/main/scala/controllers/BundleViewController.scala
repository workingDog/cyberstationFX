package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXListView, JFXSpinner, JFXTextField}
import com.kodekutters.stix.{Bundle, Identifier}
import cyber.{CyberBundle, CyberObj, InfoTableEntry}
import taxii.{Collection, TaxiiCollection}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafx.Includes._
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty, StringProperty}
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

  def setSelectedCollection(theSelectedCollection: ObjectProperty[TaxiiCollection]): Unit

  def addStixToBundle(stix: CyberObj)

  def removeStixFromBundle(stix: CyberObj)

  def getCurrentBundle(): ReadOnlyObjectProperty[CyberBundle]

  def getBundleStixView(): JFXListView[CyberObj]

  def getAllBundles(): List[Bundle]
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

  val connInfo = ObservableBuffer[InfoTableEntry]()
  var taxiiApiroot: Option[String] = None
  var taxiiCol: Option[TaxiiCollection] = None

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
    theSelectedApiroot.onChange { (_, oldValue, newValue) => {
      if (newValue != null) {
        taxiiApiroot = Option(newValue)
        connInfo.update(1, new InfoTableEntry("Api root", newValue))
      } else {
        taxiiApiroot = None
      }
    }
    }
  }

  override def setSelectedCollection(theSelectedCollection: ObjectProperty[TaxiiCollection]) {
    theSelectedCollection.onChange { (_, oldValue, newValue) =>
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
    if (bundleList.isEmpty) {
      bundleList += new CyberBundle()
      bundlesListView.getSelectionModel.selectFirst()
    }

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
    sendButton.setDisable(true)
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
          val col = Collection(colInfo, apiroot)
          col.addObjects(theBundle.toStix)
          col.conn.close()
          serverSpinner.setVisible(false)
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

  override def getAllBundles(): List[Bundle] = {
    (for(item <- bundlesListView.getItems) yield item.toStix).toList
  }

}

