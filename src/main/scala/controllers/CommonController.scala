package controllers

import java.text.NumberFormat
import javafx.fxml.FXML

import com.jfoenix.controls._
import com.kodekutters.stix.{Identifier, Timestamp}
import cyber.{CyberObj, ExtRefItem, LabelItem}
import util.Utils

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.TextFormatter
import scalafx.scene.control.cell.{CheckBoxListCell, TextFieldListCell}
import scalafx.scene.input.MouseEvent
import scalafx.util.converter.NumberStringConverter
import scalafxml.core.macros.sfxml


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
                       @FXML labelsView: JFXListView[LabelItem],
                       @FXML createdByField: JFXTextField,
                       @FXML addMarkingButton: JFXButton,
                       @FXML deleteMarkingButton: JFXButton,
                       @FXML addExtRefButton: JFXButton,
                       @FXML deleteExtRefButton: JFXButton,
                       @FXML objectMarkingsView: JFXListView[String],
                       @FXML externalRefsView: JFXListView[ExtRefItem]) extends CommonControllerInterface {

  var currentForm: CyberObj = null
  val labelsData = ObservableBuffer[LabelItem](for (lbl <- Utils.commonLabels) yield LabelItem(false, lbl, currentForm))

  init()

  def init(): Unit = {
    // make sure only integers can be in the confidenceField
    confidenceField.textFormatter = new TextFormatter(new NumberStringConverter(NumberFormat.getIntegerInstance))
    //
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
      val toAdd = ExtRefItem(false, "xxxxxx", currentForm)
      externalRefsView.getItems.add(toAdd)
    })
    deleteExtRefButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = externalRefsView.getSelectionModel.getSelectedItem
      externalRefsView.getItems.remove(toRemove)
    })
    objectMarkingsView.cellFactory = TextFieldListCell.forListView()
    addMarkingButton.setOnMouseClicked((ev: MouseEvent) => {
      if (currentForm != null) currentForm.object_marking_refs += Utils.randName
    })
    deleteMarkingButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = objectMarkingsView.getSelectionModel.getSelectedItem
      if (currentForm != null) currentForm.object_marking_refs -= toRemove
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
    objectMarkingsView.setItems(null)
    //  externalRefsView.setItems(null)
    revokedField.setSelected(false)
    idField.setText("")
  }

  private def loadValues(): Unit = {
    createdField.setText(currentForm.created.value)
    modifiedField.setText(currentForm.modified.value)
    confidenceField.setText(currentForm.confidence.value)
    langField.setText(currentForm.lang.value)
    labelsView.getItems.foreach(item => {
      item.form = currentForm
      if (currentForm.labels.contains(item.name))
        item.selected.value = true
      else
        item.selected.value = false
    })
    objectMarkingsView.setItems(currentForm.object_marking_refs)
    //  externalRefsView.items = currentForm.external_references
    createdByField.setText(currentForm.created_by_ref.value)
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
      objectMarkingsView.setItems(null)
      labelsView.getItems.foreach(item => {
        item.form = null
        item.selected.value = false
      })
      externalRefsView.getItems.foreach(item => {
        item.form = null
        item.selected.value = false
      })
      currentForm = null
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
      currentForm.confidence <== confidenceField.textProperty()
      currentForm.created_by_ref <== createdByField.textProperty()
      currentForm.revoked <== revokedField.selectedProperty()
      currentForm.id <== idField.textProperty()
      // the new id button action
      idButton.setOnMouseClicked((_: MouseEvent) => {
        if (currentForm != null) {
          idField.setText(Identifier(currentForm.`type`.value).toString())
          // force a refresh
          controller.map(_.getBundleStixView.refresh())
        }
      })
    }
  }

}