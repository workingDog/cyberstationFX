package controllers

import java.text.NumberFormat
import javafx.fxml.FXML
import java.io.IOException
import javafx.scene.text.Text

import com.jfoenix.controls._
import com.kodekutters.stix.{Identifier, Timestamp}
import cyber._
import util.CyberUtils

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.scene.Scene
import scalafx.scene.control.{ListCell, TextFormatter}
import scalafx.scene.control.cell.{CheckBoxListCell, TextFieldListCell}
import scalafx.scene.input.{MouseButton, MouseEvent}
import scalafx.stage.{Modality, Stage}
import scalafx.util.converter.NumberStringConverter
import scalafxml.core.{DependenciesByType, FXMLLoader}
import scalafxml.core.macros.{nested, sfxml}


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
                       @FXML externalRefsView: JFXListView[ExternalRefForm]) extends CommonControllerInterface {

  var currentForm: CyberObj = null
  val labelsData = ObservableBuffer[LabelItem](for (lbl <- CyberUtils.commonLabels) yield LabelItem(false, lbl, currentForm))

  init()

  def init(): Unit = {
    // make sure only integers can be entered in the confidenceField
    confidenceField.textFormatter = new TextFormatter(new NumberStringConverter(NumberFormat.getIntegerInstance))
    // labels list
    labelsView.setItems(labelsData)
    labelsView.cellFactory = CheckBoxListCell.forListView(_.selected)
    // created and modified timestamps
    renewCreated.setOnMouseClicked((_: MouseEvent) => {
      createdField.setText(Timestamp.now().toString())
    })
    renewModified.setOnMouseClicked((_: MouseEvent) => {
      modifiedField.setText(Timestamp.now().toString())
    })
    // external references
    externalRefsView.cellFactory = { _ =>
      new ListCell[ExternalRefForm] {
        item.onChange { (_, _, extRef) =>
          if (extRef != null) text = extRef.source_name.value
          else text = ""
        }
      }
    }
    addExtRefButton.setOnMouseClicked((_: MouseEvent) => {
      if (currentForm != null) {
        val newForm = new ExternalRefForm() {
          source_name.value = "source_name"
        }
        if (showExtRefDialog(newForm)) currentForm.external_references += newForm
      }
    })
    deleteExtRefButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = externalRefsView.getSelectionModel.getSelectedItem
      if (currentForm != null) currentForm.external_references -= toRemove
    })
    // double click on a externalRefsView entry to edit the selected external reference
    externalRefsView.setOnMouseClicked((event: MouseEvent) => {
      if ((event.button == MouseButton.Primary) && (event.clickCount == 2) && event.getTarget.isInstanceOf[Text]) {
        if (currentForm != null) {
          showExtRefDialog(externalRefsView.getSelectionModel.getSelectedItem)
          externalRefsView.refresh()
        }
      }
    })

    // object markings list
    objectMarkingsView.cellFactory = TextFieldListCell.forListView()
    addMarkingButton.setOnMouseClicked((ev: MouseEvent) =>
      if (currentForm != null) currentForm.object_marking_refs += ("xx--" + CyberUtils.randName)
    )
    deleteMarkingButton.setOnMouseClicked((_: MouseEvent) => {
      val toRemove = objectMarkingsView.getSelectionModel.getSelectedItem
      if (currentForm != null) currentForm.object_marking_refs -= toRemove
    })
  }

  def clear(): Unit = {
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
    externalRefsView.setItems(null)
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
    externalRefsView.setItems(currentForm.external_references)
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
      externalRefsView.items.unbind()
      externalRefsView.setItems(null)
      currentForm = null
    }
  }

  def control(stix: CyberObj, controller: Option[BundleViewControllerInterface]): Unit = {
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

  // popup the external reference editor dialog
  def showExtRefDialog(extRefForm: ExternalRefForm): Boolean =
    try {
      // load the fxml file
      val resource = CyberStationApp.getClass.getResource("forms/extRefDialog.fxml")
      if (resource == null) {
        throw new IOException("Cannot load resource: forms/extRefDialog.fxml")
      }
      val loader = new FXMLLoader(resource, new DependenciesByType(Map.empty))
      val pane = loader.load.asInstanceOf[javafx.scene.layout.GridPane]
      val scene = new Scene(pane)
      // create the dialog Stage
      val theStage = new Stage()
      theStage.setTitle("external reference")
      theStage.initModality(Modality.WindowModal)
      theStage.initOwner(CyberStationApp.stage)
      theStage.setScene(scene)
      // give the stage and external reference to the controller
      val controller = loader.getController[ExternalRefControllerInterface]()
      controller.setDialogStage(theStage)
      controller.setExternalRef(extRefForm)
      // show the dialog and wait until the user closes it
      theStage.showAndWait
      // return true if the ok button was clicked else false
      controller.isOkClicked
    } catch {
      case e: IOException =>
        e.printStackTrace()
        false
    }

}