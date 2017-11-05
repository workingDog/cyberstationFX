package controllers

import javafx.fxml.FXML
import com.jfoenix.controls._
import cyberProtocol.CyberObj
import scalafxml.core.macros.sfxml


trait CommonControllerInterface {
  def control(stix: CyberObj): Unit
  def clear(): Unit
}

@sfxml
class CommonController(@FXML createdField: JFXTextField,
                       @FXML modifiedField: JFXTextField,
                       @FXML revokedField: JFXToggleButton,
                       @FXML confidenceField: JFXTextField,
                       @FXML langField: JFXTextField,
                       @FXML labelsField: JFXComboBox[String],
                       @FXML createdByField: JFXTextField,
                       @FXML objMarkingsField: JFXTextField,
                       @FXML externalRefField: JFXTextField) extends CommonControllerInterface {

  var currentForm: CyberObj = null

  override def clear(): Unit = {
    unbindAll()
    createdField.setText("")
    modifiedField.setText("")
    confidenceField.setText("")
    langField.setText("")
    createdByField.setText("")
    objMarkingsField.setText("")
    externalRefField.setText("")
    revokedField.setSelected(false)
  }

  private def loadValues(): Unit = {
    createdField.setText(currentForm.created.value)
    modifiedField.setText(currentForm.modified.value)
    confidenceField.setText(currentForm.confidence.value.toString())
    langField.setText(currentForm.lang.value)
    createdByField.setText(currentForm.created_by_ref.value)
    objMarkingsField.setText("")
    externalRefField.setText("")
    revokedField.setSelected(currentForm.revoked.value)
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
      //  currentForm.labels.unbind()
      //    currentForm.confidence.unbind()
      //  currentForm.external_references.unbind()
      //  currentForm.object_marking_refs.unbind()
      //  currentForm.granular_markings.unbind()
    }
  }

  override def control(stix: CyberObj): Unit = {
    unbindAll()
    currentForm = stix
    loadValues()
    // bind the form to the UI
    currentForm.lang <== langField.textProperty()
    currentForm.created <== createdField.textProperty()
    currentForm.modified <== modifiedField.textProperty()
    //form.confidence <== confidenceField.IntegerProperty()
    currentForm.created_by_ref <== createdByField.textProperty()
    currentForm.revoked <== revokedField.selectedProperty()
  }

}