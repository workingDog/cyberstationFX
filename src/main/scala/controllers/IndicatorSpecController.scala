package controllers

import java.io.IOException
import javafx.fxml.FXML
import javafx.scene.text.Text

import com.jfoenix.controls.{JFXButton, JFXListView, JFXTextArea, JFXTextField}
import com.kodekutters.stix.Timestamp
import cyber.{CyberStationApp, IndicatorForm, KillChainPhaseForm}

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.control.ListCell
import scalafx.scene.input.{MouseButton, MouseEvent}
import scalafx.stage.{Modality, Stage}
import scalafxml.core.{DependenciesByType, FXMLLoader}
import scalafxml.core.macros.sfxml


trait IndicatorSpecControllerInterface {
  def control(stix: IndicatorForm, controller: Option[BundleViewControllerInterface]): Unit

  def clear(): Unit
}

@sfxml
class IndicatorSpecController(@FXML patternField: JFXTextField,
                              @FXML validFromButton: JFXButton,
                              @FXML validFromField: JFXTextField,
                              @FXML validUntilButton: JFXButton,
                              @FXML validUntilField: JFXTextField,
                              @FXML descriptionField: JFXTextArea,
                              @FXML deleteKFButton: JFXButton,
                              @FXML addKFButton: JFXButton,
                              @FXML killPhaseListView: JFXListView[KillChainPhaseForm]
                             ) extends IndicatorSpecControllerInterface {

  var currentForm: IndicatorForm = null

  init()

  def init(): Unit = {
    validFromButton.setOnMouseClicked((_: MouseEvent) => {
      validFromField.setText(Timestamp.now().toString())
    })
    validUntilButton.setOnMouseClicked((_: MouseEvent) => {
      validUntilField.setText(Timestamp.now().toString())
    })
    // kill chain phases
    killPhaseListView.cellFactory = { _ =>
      new ListCell[KillChainPhaseForm] {
        item.onChange { (_, _, kcf) =>
          if (kcf != null) text = kcf.kill_chain_name.value + " --> " + kcf.phase_name.value
          else text = ""
        }
      }
    }
    addKFButton.setOnMouseClicked((ev: MouseEvent) => {
      if (currentForm != null) {
        val newForm = new KillChainPhaseForm() {
          kill_chain_name.value = ""
          phase_name.value = ""
        }
        if (showKillChainDialog(newForm)) currentForm.kill_chain_phases += newForm
      }
    })
    deleteKFButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = killPhaseListView.getSelectionModel.getSelectedItem
      if (currentForm != null) currentForm.kill_chain_phases -= toRemove
    })
    // double click on a killPhaseListView entry to edit the selected kill_chain_phase
    killPhaseListView.setOnMouseClicked((event: MouseEvent) => {
      if ((event.button == MouseButton.Primary) && (event.clickCount == 2) && event.getTarget.isInstanceOf[Text]) {
        if (currentForm != null) {
          showKillChainDialog(killPhaseListView.getSelectionModel.getSelectedItem)
          killPhaseListView.refresh()
        }
      }
    })

  }

  private def loadValues(): Unit = {
    validFromField.setText(currentForm.valid_from.value)
    validUntilField.setText(currentForm.valid_until.value)
    patternField.setText(currentForm.pattern.value)
    descriptionField.setText(currentForm.description.value)
    killPhaseListView.setItems(currentForm.kill_chain_phases)
  }

  def clear(): Unit = {
    unbindAll()
    validFromField.setText("")
    validUntilField.setText("")
    patternField.setText("")
    descriptionField.setText("")
    killPhaseListView.setItems(null)
  }

  private def unbindAll(): Unit = {
    if (currentForm != null) {
      currentForm.valid_from.unbind()
      currentForm.valid_until.unbind()
      currentForm.pattern.unbind()
      currentForm.description.unbind()
      killPhaseListView.items.unbind()
      killPhaseListView.setItems(null)
      currentForm = null
    }
  }

  def control(stix: IndicatorForm, controller: Option[BundleViewControllerInterface]): Unit = {
    unbindAll()
    if (stix != null) {
      currentForm = stix
      loadValues()
      // bind the form to the UI
      currentForm.valid_from <== validFromField.textProperty()
      currentForm.valid_until <== validUntilField.textProperty()
      currentForm.pattern <== patternField.textProperty()
      currentForm.description <== descriptionField.textProperty()
    }
  }

  // popup the external reference editor dialog
  def showKillChainDialog(killChainForm: KillChainPhaseForm): Boolean =
    try {
      // record the initial values, in case we cancel
      val formCopy = KillChainPhaseForm.clone(killChainForm)
      // load the fxml file
      val resource = CyberStationApp.getClass.getResource("forms/killChainDialog.fxml")
      if (resource == null) {
        throw new IOException("Cannot load resource: forms/killChainDialog.fxml")
      }
      val loader = new FXMLLoader(resource, new DependenciesByType(Map.empty))
      val pane = loader.load.asInstanceOf[javafx.scene.layout.GridPane]
      val scene = new Scene(pane)
      // create the dialog Stage
      val theStage = new Stage()
      theStage.setTitle("kill chain phase")
      theStage.initModality(Modality.WindowModal)
      theStage.initOwner(CyberStationApp.stage)
      theStage.setScene(scene)
      val controller = loader.getController[KillChainControllerInterface]()
      controller.setDialogStage(theStage)
      controller.setKillChainPhase(killChainForm)
      // show the dialog and wait until the user closes it
      theStage.showAndWait
      // if cancel, reset to the previous values
      if (!controller.isOkClicked()) {
        killChainForm.kill_chain_name.value = formCopy.kill_chain_name.value
        killChainForm.phase_name.value = formCopy.phase_name.value
      }
      // return true if the ok button was clicked else false
      controller.isOkClicked()
    } catch {
      case e: IOException =>
        e.printStackTrace()
        false
    }

}