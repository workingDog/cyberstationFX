package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXListView, JFXTextArea, JFXTextField}
import com.kodekutters.stix.Indicator
import cyber.{CyberObj, _}

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.scene.control.Label
import scalafx.scene.input.MouseEvent
import scalafxml.core.macros.{nested, sfxml}


/**
  * the Indicator controller
  */
trait IndicatorControllerInterface extends BaseControllerInterface

@sfxml
class IndicatorController(@FXML theListView: JFXListView[IndicatorForm],
                          @FXML addButton: JFXButton,
                          @FXML deleteButton: JFXButton,
                          bundleLabel: Label,
                          @nested[CommonController] commonController: CommonControllerInterface,
                          @nested[IndicatorSpecController] indicatorSpecController: IndicatorSpecControllerInterface)
  extends IndicatorControllerInterface {

  // the base controller for the common properties
  val baseForm = new BaseFormController(Indicator.`type`, theListView.asInstanceOf[JFXListView[CyberObj]], bundleLabel, commonController, indicatorSpecController)

  // give the base controller the BundleViewControllerInterface, so it can refer to it
  def setBundleViewController(controller: BundleViewControllerInterface): Unit = baseForm.setBundleViewController(controller)

  deleteButton.setOnMouseClicked((_: MouseEvent) => baseForm.doDelete())
  addButton.setOnMouseClicked((_: MouseEvent) => baseForm.doAdd(new IndicatorForm()))

}

/**
  * the controller for all Indicator specific properties, i.e. other than the common ones
  */
trait IndicatorSpecControllerInterface extends BaseSpecControllerInterface

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

  var currentForm: IndicatorForm = _

  val killPhaseSupport = new KillChainPhaseHelper(killPhaseListView, addKFButton, deleteKFButton)

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

  def control(stix: CyberObj, controller: Option[BundleViewControllerInterface]): Unit = {
    unbindAll()
    if (stix != null) {
      currentForm = stix.asInstanceOf[cyber.IndicatorForm]
      killPhaseSupport.setCurrentForm(currentForm)
      loadValues()
      // bind the form to the UI
      currentForm.valid_from <== validFromField.textProperty()
      currentForm.valid_until <== validUntilField.textProperty()
      currentForm.pattern <== patternField.textProperty()
      currentForm.description <== descriptionField.textProperty()
    }
  }

}