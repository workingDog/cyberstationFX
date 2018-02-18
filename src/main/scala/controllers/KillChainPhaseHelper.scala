package controllers

import java.io.IOException
import javafx.scene.text.Text

import com.jfoenix.controls.{JFXButton, JFXListView}
import cyber._

import scalafx.stage.{Modality, Stage}
import scalafxml.core.{DependenciesByType, FXMLLoader}
import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.control.ListCell
import scalafx.scene.input.{MouseButton, MouseEvent}


class KillChainPhaseHelper(killPhaseListView: JFXListView[KillChainPhaseForm],
                           addKFButton: JFXButton, deleteKFButton: JFXButton) {

  var currentForm: KillChainPhaseTrait = _

  init()


  def setCurrentForm(theForm: KillChainPhaseTrait): Unit = currentForm = theForm

  def init(): Unit = {
    deleteKFButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = killPhaseListView.getSelectionModel.getSelectedItem
      if (currentForm != null) currentForm.kill_chain_phases -= toRemove
    })
    addKFButton.setOnMouseClicked((_: MouseEvent) => {
      if (currentForm != null) {
        val newForm = new KillChainPhaseForm()
        if (showKillChainDialog(newForm)) currentForm.kill_chain_phases += newForm
      }
    })
    killPhaseListView.cellFactory = { _ =>
      new ListCell[KillChainPhaseForm] {
        item.onChange { (_, _, kcf) =>
          if (kcf != null) text = kcf.kill_chain_name.value + " --> " + kcf.phase_name.value
          else text = ""
        }
      }
    }
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
