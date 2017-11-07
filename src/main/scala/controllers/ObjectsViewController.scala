package controllers

import cyberProtocol.{CyberConverter, CyberObj}
import taxii.{Collection, TaxiiCollection}
import scala.concurrent.ExecutionContext.Implicits.global

import scalafxml.core.macros.sfxml
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.{TableColumn, TableView}


trait ObjectsViewControllerInterface {
  def init(): Unit

  def setSelectedApiroot(theSelectedApiroot: StringProperty): Unit

  def setSelectedCollection(theSelectedCollection: ObjectProperty[TaxiiCollection]): Unit
}

@sfxml
class ObjectsViewController(objectsTable: TableView[CyberObj]) extends ObjectsViewControllerInterface {

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
        sortable = false
        cellValueFactory = _.value.`type`
      },
      new TableColumn[CyberObj, String]() {
        text = "Created"
        prefWidth = 180
        editable = false
        sortable = false
        cellValueFactory = _.value.created
      },
      new TableColumn[CyberObj, String]() {
        text = "Id"
        prefWidth = 350
        editable = false
        sortable = false
        cellValueFactory = _.value.id
      })
  }

  def getObjects(taxiiCol: TaxiiCollection): Unit = {
    objects.clear()
    if (taxiiCol == null) return
    if (taxiiCol.id != null && apirootInfo != null) {
      val col = new Collection(apirootInfo, apirootInfo, taxiiCol.id, "", "")
      col.getObjects().map(bndl => {
        bndl.map(theBundle =>
          for (obj <- theBundle.objects) objects.append(CyberConverter.fromStix(obj)))
      })
    }
  }

}
