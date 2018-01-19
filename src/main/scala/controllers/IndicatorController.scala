package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXListView}
import com.kodekutters.stix._
import cyber.{CyberObj, IndicatorForm}

import scalafx.Includes._
import scalafx.scene.control.Label
import scalafx.scene.input.MouseEvent
import scalafxml.core.macros.{nested, sfxml}


trait IndicatorControllerInterface {
  def init(): Unit

  def setBundleViewController(controller: BundleViewControllerInterface): Unit
}


@sfxml
class IndicatorController(@FXML indicatorListView: JFXListView[IndicatorForm],
                          @FXML addButton: JFXButton,
                          @FXML deleteButton: JFXButton,
                          bundleLabel: Label,
                          @nested[CommonController] commonController: CommonControllerInterface,
                          @nested[IndicatorSpecController] indicatorSpecController: IndicatorSpecControllerInterface)
  extends IndicatorControllerInterface {

  val baseForm = new BaseFormController(Indicator.`type`, indicatorListView.asInstanceOf[JFXListView[CyberObj]], addButton, deleteButton, bundleLabel, commonController)
  def setBundleViewController(controller: BundleViewControllerInterface): Unit = baseForm.setBundleViewController(controller)
  deleteButton.setOnMouseClicked((_: MouseEvent) => baseForm.doDelete())
  addButton.setOnMouseClicked((_: MouseEvent) => baseForm.doAdd(new IndicatorForm()))

  init()

  def init(): Unit = {
    baseForm.init()
    indicatorListView.getSelectionModel.selectedItem.onChange {
      (_, oldValue, newValue) =>
        if (newValue != null) {
          indicatorSpecController.control(newValue, baseForm.bundleController)
        } else {
          indicatorSpecController.clear()
        }
    }
  }

}
