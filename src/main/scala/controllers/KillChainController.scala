package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXTextField}
import cyber.KillChainPhaseForm

import scalafx.stage.Stage
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.scene.input.MouseEvent


trait KillChainControllerInterface {
  def setDialogStage(theStage: Stage): Unit

  def setKillChainPhase(extRef: KillChainPhaseForm): Unit

  def isOkClicked(): Boolean
}

@sfxml
class KillChainController(@FXML okButton: JFXButton,
                          @FXML cancelButton: JFXButton,
                          @FXML killChainNameField: JFXTextField,
                          @FXML phaseNameField: JFXTextField) extends KillChainControllerInterface {

  var theForm: KillChainPhaseForm = _
  var dialogStage: Stage = _
  var isOk = false

  def isOkClicked(): Boolean = isOk

  def setDialogStage(theStage: Stage): Unit = {
    dialogStage = theStage
  }

  def setKillChainPhase(extRef: KillChainPhaseForm): Unit = {
    clear()
    if (extRef != null) {
      theForm = extRef
      loadValues()
      // bind the form to the UI
      theForm.kill_chain_name <== killChainNameField.textProperty()
      theForm.phase_name <== phaseNameField.textProperty()
      // must have some text for kill_chain_name and phase_name
      okButton.disableProperty().bind(theForm.kill_chain_name.isEmpty() || theForm.phase_name.isEmpty())
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
    killChainNameField.setText(theForm.kill_chain_name.value)
    phaseNameField.setText(theForm.phase_name.value)
  }

  private def unbindAll(): Unit = {
    if (theForm != null) {
      theForm.kill_chain_name.unbind()
      theForm.phase_name.unbind()
    }
  }

  private def clear(): Unit = {
    unbindAll()
    killChainNameField.setText("")
    phaseNameField.setText("")
  }

}
