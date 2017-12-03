package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXListView, JFXTextArea, JFXTextField}
import cyber.ExternalRefForm

import scalafx.stage.Stage
import scalafxml.core.macros.{nested, sfxml}
import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.collections.ObservableBuffer
import scalafx.scene.input.MouseEvent


trait ExternalRefControllerInterface {
  def setDialogStage(theStage: Stage): Unit

  def setExternalRef(extRef: ExternalRefForm): Unit

  def isOkClicked: Boolean
}

@sfxml
class ExternalRefController(@FXML hashesListView: JFXListView[String],
                            @FXML okButton: JFXButton,
                            @FXML cancelButton: JFXButton,
                            @FXML externalIdField: JFXTextField,
                            @FXML urlField: JFXTextField,
                            @FXML sourceNameField: JFXTextField,
                            @FXML descriptionField: JFXTextArea) extends ExternalRefControllerInterface {

  var theForm: ExternalRefForm = _
  var dialogStage: Stage = _
  var isOk = false

  def isOkClicked: Boolean = isOk

  def setDialogStage(theStage: Stage): Unit = {
    dialogStage = theStage
  }

  def setExternalRef(extRef: ExternalRefForm): Unit = {
    clear()
    if (extRef != null) {
      theForm = extRef
      loadValues()
      // bind the form to the UI
      theForm.source_name <== sourceNameField.textProperty()
      theForm.url <== urlField.textProperty()
      theForm.description <== descriptionField.textProperty()
      theForm.external_id <== externalIdField.textProperty()
      //  theForm.hashes <== createdByField.textProperty()
      // must have some text for source_name
      okButton.disableProperty().bind(
        Bindings.createBooleanBinding(() =>
          theForm.source_name.value.trim().isEmpty(), sourceNameField.textProperty())
      )
    }
  }

  okButton.setOnMouseClicked((_: MouseEvent) => {
    isOk = true
    clear()
    dialogStage.close()
  })

  cancelButton.setOnMouseClicked((_: MouseEvent) => {
    isOk = false
    unbindAll()
    dialogStage.close()
  })

  private def loadValues(): Unit = {
    sourceNameField.setText(theForm.source_name.value)
    urlField.setText(theForm.url.value)
    externalIdField.setText(theForm.external_id.value)
    descriptionField.setText(theForm.description.value)
    //  hashesListView.getItems.foreach(item => {
    //      item.form = theForm
    //      if (theForm.hashes.contains(item))
    //        item.selected.value = true
    //      else
    //        item.selected.value = false
    //  })
    hashesListView.setItems(ObservableBuffer[String]("hashes to do"))
  }

  private def unbindAll(): Unit = {
    if (theForm != null) {
      theForm.source_name.unbind()
      theForm.url.unbind()
      theForm.external_id.unbind()
      theForm.description.unbind()
      // hashes
    }
  }

  private def clear(): Unit = {
    unbindAll()
    sourceNameField.setText("")
    urlField.setText("")
    externalIdField.setText("")
    descriptionField.setText("")
  }

}
