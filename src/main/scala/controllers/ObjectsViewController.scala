package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.JFXSpinner
import cyber.{CyberConverter, CyberObj}
import taxii.{Collection, TaxiiCollection}

import scala.concurrent.ExecutionContext.Implicits.global
import scalafx.application.Platform
import scalafxml.core.macros.sfxml
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.{Label, TableColumn, TableView}


trait ObjectsViewControllerInterface {
  def init(): Unit

  def setSelectedApiroot(theSelectedApiroot: StringProperty): Unit

  def setSelectedCollection(theSelectedCollection: ObjectProperty[TaxiiCollection]): Unit
}

@sfxml
class ObjectsViewController(objCountLabel: Label,
                            objectsTable: TableView[CyberObj],
                            @FXML objSpinner: JFXSpinner) extends ObjectsViewControllerInterface {

  val objects = ObservableBuffer[CyberObj]()
  var apirootInfo = ""

  init()

  override def setSelectedApiroot(theSelectedApiroot: StringProperty) {
    theSelectedApiroot.onChange { (_, oldValue, newValue) =>
      apirootInfo = newValue
    }
  }

  override def setSelectedCollection(theSelectedCollection: ObjectProperty[TaxiiCollection]) {
    theSelectedCollection.onChange { (_, oldValue, newValue) =>
      getObjects(newValue)
    }
  }

  override def init(): Unit = {
    objSpinner.setVisible(false)
    // setup the table of objects
    objectsTable.setItems(objects)
    objectsTable.editable = false
    objectsTable.selectionModel = null
    objectsTable.columns.clear()
    objectsTable.columns ++= Seq(
      new TableColumn[CyberObj, String] {
        text = "Type"
        prefWidth = 150
        editable = false
        cellValueFactory = _.value.`type`
      },
      new TableColumn[CyberObj, String]() {
        text = "Name"
        prefWidth = 200
        editable = false
        cellValueFactory = _.value.name
      },
      new TableColumn[CyberObj, String]() {
        text = "Created"
        prefWidth = 180
        editable = false
        cellValueFactory = _.value.created
      },
      new TableColumn[CyberObj, String]() {
        text = "Created by"
        prefWidth = 350
        editable = false
        cellValueFactory = _.value.created_by_ref
      },
      new TableColumn[CyberObj, String]() {
        text = "Id"
        prefWidth = 350
        editable = false
        cellValueFactory = _.value.id
      })
  }

  def getObjects(taxiiCol: TaxiiCollection): Unit = {
    objSpinner.setVisible(true)
    objects.clear()
    objCountLabel.setText("")
    if (taxiiCol == null) return
    if (taxiiCol.id != null && apirootInfo != null) {
      val col = Collection(taxiiCol, apirootInfo)
      col.getObjects().map(bndl => {
        bndl.map(theBundle => {
          for (stix <- theBundle.objects) objects.append(CyberConverter.toCyberObj(stix))
          col.conn.close()
          Platform.runLater(() => {
            objSpinner.setVisible(false)
            val count = (theBundle.objects.length).toString
            objCountLabel.setText(count + " objects")
          })
        })
      })
    } else objSpinner.setVisible(false)
  }

}
