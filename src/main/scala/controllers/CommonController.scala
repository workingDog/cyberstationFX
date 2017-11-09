package controllers

import javafx.fxml.FXML

import com.jfoenix.controls._
import com.kodekutters.stix.Identifier
import cyber.CyberObj

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.SelectionMode
import scalafx.scene.input.MouseEvent
import scalafxml.core.macros.sfxml



trait CommonControllerInterface {
  def control(stix: CyberObj, controller: Option[BundleViewControllerInterface]): Unit

  def clear(): Unit
}

@sfxml
class CommonController(@FXML idButton: JFXButton,
                       @FXML idField: JFXTextField,
                       @FXML createdField: JFXTextField,
                       @FXML modifiedField: JFXTextField,
                       @FXML revokedField: JFXToggleButton,
                       @FXML confidenceField: JFXTextField,
                       @FXML langField: JFXTextField,
                       @FXML labelsView: JFXListView[String],
                       @FXML createdByField: JFXTextField,
                       @FXML objMarkingsField: JFXTextField,
                       @FXML externalRefField: JFXTextField) extends CommonControllerInterface {

  var currentForm: CyberObj = null
  var controller: Option[BundleViewControllerInterface] = None
  val initLabels = ObservableBuffer[String]("", "anomalous-activity", "anonymization", "benign",
    "organization", "compromised", "malicious-activity", "attribution")

  init()

  def init(): Unit = {
    labelsView.getSelectionModel.selectionMode = SelectionMode.Multiple
    labelsView.getSelectionModel.selectedItems.onChange { (oldList, newList) =>
      currentForm.labels.clear()
      currentForm.labels.appendAll(labelsView.getSelectionModel.getSelectedItems)
      println("--> labelsView " + currentForm.labels.toList)
    }
  }

  override def clear(): Unit = {
    unbindAll()
    createdField.setText("")
    modifiedField.setText("")
    confidenceField.setText("")
    langField.setText("")
    labelsView.setItems(ObservableBuffer[String]())
    createdByField.setText("")
    objMarkingsField.setText("")
    externalRefField.setText("")
    revokedField.setSelected(false)
    idField.setText("")
  }

  private def loadValues(): Unit = {
    createdField.setText(currentForm.created.value)
    modifiedField.setText(currentForm.modified.value)
    confidenceField.setText(currentForm.confidence.value.toString())
    langField.setText(currentForm.lang.value)
    //  labelsView.setItems(currentForm.labels)
    labelsView.setItems(initLabels)
    createdByField.setText(currentForm.created_by_ref.value)
    objMarkingsField.setText("")
    externalRefField.setText("")
    revokedField.setSelected(currentForm.revoked.value)
    idField.setText(currentForm.id.value)
  }

  private def unbindAll(): Unit = {
    if (currentForm != null) {
      //  currentForm.getClass.getFields.foreach(f => f.unbind())
      currentForm.lang.unbind()
      currentForm.created.unbind()
      currentForm.modified.unbind()
      currentForm.confidence.unbind()
      currentForm.created_by_ref.unbind()
      currentForm.revoked.unbind()
      currentForm.id.unbind()
      currentForm.labels.clear()
      //  currentForm.external_references.unbind()
      //  currentForm.object_marking_refs.unbind()
      //  currentForm.granular_markings.unbind()
    }
  }

  override def control(stix: CyberObj, controllerOpt: Option[BundleViewControllerInterface]): Unit = {
    unbindAll()
    currentForm = stix
    controller = controllerOpt
    loadValues()
    // bind the form to the UI
    currentForm.lang <== langField.textProperty()
    currentForm.created <== createdField.textProperty()
    currentForm.modified <== modifiedField.textProperty()
    //form.confidence <== confidenceField.IntegerProperty()
    currentForm.created_by_ref <== createdByField.textProperty()
    currentForm.revoked <== revokedField.selectedProperty()
    currentForm.id <== idField.textProperty()
  }

  idButton.setOnMouseClicked((_: MouseEvent) => {
    if (currentForm != null) {
      idField.setText(Identifier(currentForm.`type`.value).toString())
      // force a refresh
      controller.map(_.getBundleStixView.refresh())
    }
  })

}