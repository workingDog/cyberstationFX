package controllers

import javafx.fxml.FXML

import com.jfoenix.controls._
import com.kodekutters.stix.{Identifier, Timestamp}
import cyber.CyberObj
import util.Utils

import scalafx.Includes._
import scalafx.beans.property.BooleanProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.cell.{CheckBoxListCell, TextFieldListCell}
import scalafx.scene.input.MouseEvent
import scalafxml.core.macros.sfxml


case class Item(init: Boolean, name: String, var form: CyberObj) {
  val selected = BooleanProperty(init)
  selected.onChange { (_, _, newValue) => {
    if (form != null)
      if (newValue) form.labels += name else form.labels -= name
    }
  }
  override def toString: String = name
}

trait CommonControllerInterface {
  def control(stix: CyberObj, controller: Option[BundleViewControllerInterface]): Unit

  def clear(): Unit
}

@sfxml
class CommonController(@FXML idButton: JFXButton,
                       @FXML idField: JFXTextField,
                       @FXML createdField: JFXTextField,
                       @FXML renewCreated: JFXButton,
                       @FXML renewModified: JFXButton,
                       @FXML modifiedField: JFXTextField,
                       @FXML revokedField: JFXToggleButton,
                       @FXML confidenceField: JFXTextField,
                       @FXML langField: JFXTextField,
                       @FXML labelsView: JFXListView[Item],
                       @FXML createdByField: JFXTextField,
                       @FXML addMarkingButton: JFXButton,
                       @FXML deleteMarkingButton: JFXButton,
                       @FXML addExtRefButton: JFXButton,
                       @FXML deleteExtRefButton: JFXButton,
                       @FXML objectMarkingsView: JFXListView[String],
                       @FXML externalRefsView: JFXListView[Item]) extends CommonControllerInterface {

  var currentForm: CyberObj = null
  var onLoad = false
  val labelsData = ObservableBuffer[Item](for (lbl <- Utils.commonLabels) yield Item(false, lbl, currentForm))

  init()

  def init(): Unit = {
    labelsView.setItems(labelsData)
    labelsView.cellFactory = CheckBoxListCell.forListView(_.selected)
    // created and modified timestamps
    renewCreated.setOnMouseClicked((_: MouseEvent) => {
      createdField.setText(Timestamp.now().toString())
    })
    renewModified.setOnMouseClicked((_: MouseEvent) => {
      modifiedField.setText(Timestamp.now().toString())
    })
    // todo external references
    addExtRefButton.setOnMouseClicked((_: MouseEvent) => {
      val toAdd = Item(false, "xxxxxx", currentForm)
      externalRefsView.getItems.add(toAdd)
    })
    deleteExtRefButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = externalRefsView.getSelectionModel.getSelectedItem
      externalRefsView.getItems.remove(toRemove)
    })
    // todo object marking references
    objectMarkingsView.cellFactory = TextFieldListCell.forListView()
    addMarkingButton.setOnMouseClicked((_: MouseEvent) => {
      val toAdd = "xxxxxxzzz"
      objectMarkingsView.getItems.add(toAdd)
    })
    deleteMarkingButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = objectMarkingsView.getSelectionModel.getSelectedItem
      objectMarkingsView.getItems.remove(toRemove)
    })
  }

  override def clear(): Unit = {
    unbindAll()
    createdField.setText("")
    modifiedField.setText("")
    confidenceField.setText("")
    langField.setText("")
    labelsView.getItems.foreach(item => {
      item.form = null
      item.selected.value = false
    })
    createdByField.setText("")
    //objMarkingsField.setText("")
   // externalRefField.setText("")
    revokedField.setSelected(false)
    idField.setText("")
  }

  private def loadValues(): Unit = {
    createdField.setText(currentForm.created.value)
    modifiedField.setText(currentForm.modified.value)
    confidenceField.setText(currentForm.confidence.value.toString)
    langField.setText(currentForm.lang.value)
    labelsView.getItems.foreach(item => {
      item.form = currentForm
      if (currentForm.labels.contains(item.name))
        item.selected.value = true
      else
        item.selected.value = false
    })
    createdByField.setText(currentForm.created_by_ref.value)
   // objMarkingsField.setText("")
  //  externalRefField.setText("")
    revokedField.setSelected(currentForm.revoked.value)
    idField.setText(currentForm.id.value)
  }

  private def unbindAll(): Unit = {
    if (currentForm != null) {
      currentForm.lang.unbind()
      currentForm.created.unbind()
      currentForm.modified.unbind()
      currentForm.confidence.unbind()
      currentForm.created_by_ref.unbind()
      currentForm.revoked.unbind()
      currentForm.id.unbind()
      currentForm = null
      labelsView.getItems.foreach(item => {
        item.form = null
        item.selected.value = false
      })
    }
  }

  override def control(stix: CyberObj, controller: Option[BundleViewControllerInterface]): Unit = {
    unbindAll()
    if (stix != null) {
      currentForm = stix
      loadValues()
      // bind the form to the UI
      currentForm.lang <== langField.textProperty()
      currentForm.created <== createdField.textProperty()
      currentForm.modified <== modifiedField.textProperty()
      //form.confidence <== confidenceField.IntegerProperty()
      currentForm.created_by_ref <== createdByField.textProperty()
      currentForm.revoked <== revokedField.selectedProperty()
      currentForm.id <== idField.textProperty()
      // set the id button new id action
      idButton.setOnMouseClicked((_: MouseEvent) => {
        if(currentForm != null) {
          idField.setText(Identifier(currentForm.`type`.value).toString())
          // force a refresh
          controller.map(_.getBundleStixView.refresh())
        }
      })
    }
  }

}