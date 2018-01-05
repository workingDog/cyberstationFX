package controllers

import java.io.IOException
import javafx.fxml.FXML
import javafx.scene.text.Text

import com.jfoenix.controls.{JFXButton, JFXListView, JFXTextArea, JFXTextField}
import cyber.{CyberStationApp, ExternalRefForm, HashesForm}

import scalafx.stage.{Modality, Stage}
import scalafxml.core.macros.{nested, sfxml}
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.control.ListCell
import scalafx.scene.input.{MouseButton, MouseEvent}
import scalafxml.core.{DependenciesByType, FXMLLoader}


trait ExternalRefControllerInterface {
  def setDialogStage(theStage: Stage): Unit

  def setExternalRef(extRef: ExternalRefForm): Unit

  def isOkClicked(): Boolean
}

@sfxml
class ExternalRefController(@FXML hashesListView: JFXListView[HashesForm],
                            @FXML addHashesButton: JFXButton,
                            @FXML deleteHashesButton: JFXButton,
                            @FXML okButton: JFXButton,
                            @FXML cancelButton: JFXButton,
                            @FXML externalIdField: JFXTextField,
                            @FXML urlField: JFXTextField,
                            @FXML sourceNameField: JFXTextField,
                            @FXML descriptionField: JFXTextArea) extends ExternalRefControllerInterface {

  var theForm: ExternalRefForm = _
  var dialogStage: Stage = _
  var isOk = false

  init()

  def init(): Unit = {
    // hashes
    hashesListView.cellFactory = { _ =>
      new ListCell[HashesForm] {
        item.onChange { (_, _, kcf) =>
          if (kcf != null) text = kcf.theKey.value + " --> " + kcf.theValue.value
          else text = ""
        }
      }
    }
    addHashesButton.setOnMouseClicked((ev: MouseEvent) => {
      if (theForm != null) {
        val newForm = new HashesForm()
        if (showHashesDialog(newForm)) theForm.hashes += newForm
      }
    })
    deleteHashesButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = hashesListView.getSelectionModel.getSelectedItem
      if (theForm != null) theForm.hashes -= toRemove
    })
    // double click on a hashesListView entry to edit the selected hashes
    hashesListView.setOnMouseClicked((event: MouseEvent) => {
      if ((event.button == MouseButton.Primary) && (event.clickCount == 2) && event.getTarget.isInstanceOf[Text]) {
        if (theForm != null) {
          showHashesDialog(hashesListView.getSelectionModel.getSelectedItem)
          hashesListView.refresh()
        }
      }
    })
  }

  def isOkClicked(): Boolean = isOk

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
      // must have some text for source_name
      okButton.disableProperty().bind(theForm.source_name.isEmpty())
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
    hashesListView.setItems(theForm.hashes)
  }

  private def unbindAll(): Unit = {
    if (theForm != null) {
      theForm.source_name.unbind()
      theForm.url.unbind()
      theForm.external_id.unbind()
      theForm.description.unbind()
      hashesListView.items.unbind()
      hashesListView.setItems(null)
      theForm = null
    }
  }

  private def clear(): Unit = {
    unbindAll()
    sourceNameField.setText("")
    urlField.setText("")
    externalIdField.setText("")
    descriptionField.setText("")
    hashesListView.setItems(null)
  }

  // popup the hashes editor dialog
  def showHashesDialog(hashesForm: HashesForm): Boolean =
    try {
      // record the initial values, in case we cancel
      val formCopy = HashesForm.clone(hashesForm)
      // load the fxml file
      val resource = CyberStationApp.getClass.getResource("forms/hashesDialog.fxml")
      if (resource == null) {
        throw new IOException("Cannot load resource: forms/hashesDialog.fxml")
      }
      val loader = new FXMLLoader(resource, new DependenciesByType(Map.empty))
      val pane = loader.load.asInstanceOf[javafx.scene.layout.GridPane]
      val scene = new Scene(pane)
      // create the dialog Stage
      val theStage = new Stage()
      theStage.setTitle("hashes")
      theStage.initModality(Modality.WindowModal)
      theStage.initOwner(CyberStationApp.stage)
      theStage.setScene(scene)
      val controller = loader.getController[HashesControllerInterface]()
      controller.setDialogStage(theStage)
      controller.setHashes(hashesForm)
      // show the dialog and wait until the user closes it
      theStage.showAndWait
      // if cancel, reset to the previous values
      if (!controller.isOkClicked()) {
        hashesForm.theKey.value = formCopy.theKey.value
        hashesForm.theValue.value = formCopy.theValue.value
      }
      // return true if the ok button was clicked else false
      controller.isOkClicked()
    } catch {
      case e: IOException =>
        e.printStackTrace()
        false
    }

}
