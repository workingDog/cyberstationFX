package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXPasswordField, JFXTextArea, JFXTextField}
import cyber.ServerForm
import support.CyberUtils

import scalafx.stage.Stage
import scalafxml.core.macros.{nested, sfxml}
import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.scene.input.MouseEvent


trait ServerDialogControllerInterface {
  def setDialogStage(theStage: Stage): Unit

  def setServerInfo(srvForm: ServerForm): Unit

  def isOkClicked(): Boolean
}

@sfxml
class ServerDialogController(@FXML okButton: JFXButton,
                             @FXML cancelButton: JFXButton,
                             @FXML nameField: JFXTextField,
                             @FXML urlField: JFXTextField,
                             @FXML userField: JFXTextField,
                             @FXML pswField: JFXPasswordField) extends ServerDialogControllerInterface {

  var theForm: ServerForm = _
  var dialogStage: Stage = _
  var isOk = false

  def isOkClicked(): Boolean = isOk

  def setDialogStage(theStage: Stage): Unit = dialogStage = theStage

  def setServerInfo(srvForm: ServerForm): Unit = {
    clear()
    if (srvForm != null) {
      theForm = srvForm
      loadValues()
      // bind the form to the UI
      theForm.name <== nameField.textProperty()
      theForm.user <== userField.textProperty()
      theForm.url <== urlField.textProperty()
      theForm.psw <== pswField.textProperty()
      // must have a valid url string
      okButton.disableProperty().bind(
        Bindings.createBooleanBinding(() =>
          !CyberUtils.urlValid(theForm.url.value.trim()), urlField.textProperty())
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
    nameField.setText(theForm.name.value)
    userField.setText(theForm.user.value)
    urlField.setText(theForm.url.value)
    pswField.setText(theForm.psw.value)
  }

  private def unbindAll(): Unit = {
    if (theForm != null) {
      theForm.name.unbind()
      theForm.user.unbind()
      theForm.url.unbind()
      theForm.psw.unbind()
    }
  }

  private def clear(): Unit = {
    unbindAll()
    nameField.setText("")
    userField.setText("")
    urlField.setText("")
    pswField.setText("")
  }

}
