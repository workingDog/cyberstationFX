package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.JFXSpinner
import com.typesafe.config.{Config, ConfigFactory}
import cyber.{CyberConverter, CyberObj}
import db.MongoDbService.{bundlesCol, bundlesInf, config, userLogCol}
import taxii.{Collection, TaxiiCollection}

import scala.concurrent.ExecutionContext.Implicits.global
import scalafx.application.Platform
import scalafxml.core.macros.sfxml
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.TableColumn._
import scalafx.scene.control.{Label, TableColumn, TableView}


trait ObjectsViewControllerInterface {
  def init(): Unit

  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit
}

@sfxml
class ObjectsViewController(objCountLabel: Label,
                            objectsTable: TableView[CyberObj],
                            @FXML objSpinner: JFXSpinner) extends ObjectsViewControllerInterface {

  val objects = ObservableBuffer[CyberObj]()
  var apirootInfo = ""

  val config: Config = ConfigFactory.load
  private var fetchNumber = 100

  init()

  def init(): Unit = {
    try {
      fetchNumber = config.getInt("taxii.objects")
    } catch {
      case e: Throwable => println("---> config taxii.objects error: " + e)
    }
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

  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit = {
    cyberStationController.getSelectedApiroot().onChange { (_, oldValue, newValue) =>
      apirootInfo = newValue
    }
    cyberStationController.getSelectedCollection().onChange { (_, oldValue, newValue) =>
      getObjects(newValue)
    }
  }

  def getObjects(taxiiCol: TaxiiCollection): Unit = {
    objects.clear()
    objCountLabel.setText("")
    if (taxiiCol == null) return
    objSpinner.setVisible(true)
    if (taxiiCol.id != null && apirootInfo != null) {
      val col = Collection(taxiiCol, apirootInfo)
      col.getObjects(range = ("0-" + fetchNumber.toString)).map(bndl => {
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
