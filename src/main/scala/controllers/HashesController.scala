package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXTextField}
import cyber.HashesForm

import scalafx.stage.Stage
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.scene.input.MouseEvent


trait HashesControllerInterface {
  def setDialogStage(theStage: Stage): Unit

  def setHashes(extRef: HashesForm): Unit

  def isOkClicked(): Boolean
}

@sfxml
class HashesController(@FXML okButton: JFXButton,
                       @FXML cancelButton: JFXButton,
                       @FXML keyField: JFXTextField,
                       @FXML valueField: JFXTextField) extends HashesControllerInterface {

  var theForm: HashesForm = _
  var dialogStage: Stage = _
  var isOk = false

  def isOkClicked(): Boolean = isOk

  def setDialogStage(theStage: Stage): Unit = {
    dialogStage = theStage
  }

  def setHashes(extRef: HashesForm): Unit = {
    clear()
    if (extRef != null) {
      theForm = extRef
      loadValues()
      // bind the form to the UI
      theForm.theKey <== keyField.textProperty()
      theForm.theValue <== valueField.textProperty()
      // must have some text for the key and value
      okButton.disableProperty().bind(theForm.theValue.isEmpty() || theForm.theKey.isEmpty())
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
    keyField.setText(theForm.theKey.value)
    valueField.setText(theForm.theValue.value)
  }

  private def unbindAll(): Unit = {
    if (theForm != null) {
      theForm.theKey.unbind()
      theForm.theValue.unbind()
    }
  }

  private def clear(): Unit = {
    unbindAll()
    keyField.setText("")
    valueField.setText("")
  }

}
