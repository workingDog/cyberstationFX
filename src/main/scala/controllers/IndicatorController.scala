package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXListView}
import com.kodekutters.stix._
import cyberProtocol.IndicatorForm

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.Label
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.scene.input.MouseEvent
import scalafx.util.StringConverter
import scalafxml.core.macros.{nested, sfxml}


trait IndicatorControllerInterface {
  def init(): Unit

  def setBundleViewController(controller: BundleViewControllerInterface): Unit
}

@sfxml
class IndicatorController(@FXML indicatorListView: JFXListView[IndicatorForm],
                          @FXML addButton: JFXButton,
                          bundleLabel: Label,
                          @FXML deleteButton: JFXButton,
                          @nested[CommonController] commonController: CommonControllerInterface) extends IndicatorControllerInterface {

  val indicatorList = ObservableBuffer[IndicatorForm]()
  var bundleController: Option[BundleViewControllerInterface] = None

  init()

  override def init() {
    // setup the list of indicators
    indicatorListView.setEditable(true)
    indicatorListView.setExpanded(true)
    indicatorListView.setDepth(1)
    indicatorListView.setItems(indicatorList)
    // to edit the name in the list by double clicking on it
    indicatorListView.cellFactory = { _ =>
      new TextFieldListCell[IndicatorForm] {
        converter = indicatorStringConverter
        item.onChange { (_, _, obj) => if (obj != null) text = obj.name.value }
      }
    }
    indicatorListView.getSelectionModel.selectedItem.onChange { (_, oldValue, newValue) =>
      // the commonController will take care of all interactions/updates with the new IndicatorForm
      if (newValue != null) {
        commonController.control(newValue, bundleController)
      } else {
        commonController.clear()
      }
    }
  }

  override def setBundleViewController(controller: BundleViewControllerInterface): Unit = {
    bundleController = Option(controller)
    // the BundleViewController currentBundle
    val currentBundle = controller.getCurrentBundle()
    val bndlName = if (currentBundle.value != null) currentBundle.value.name.value else ""
    bundleLabel.text = "Part of bundle: " + bndlName
    if (currentBundle != null) {
      currentBundle.onChange { (source, oldValue, newValue) =>
        // should load the new bundle list of IndicatorForm here
        if (newValue != null) {
          val indicators = newValue.objects.filter(stix => stix.`type` == Indicator.`type`).asInstanceOf[ObservableBuffer[IndicatorForm]]
          indicatorList.clear()
          indicatorList.appendAll(indicators)
          bundleLabel.text = "Part of bundle: " + newValue.name.value
        } else {
          bundleLabel.text = "Part of bundle: "
        }
      }
    }
  }

  addButton.setOnMouseClicked((_: MouseEvent) => {
    bundleController.map(controller => {
      if (controller.getCurrentBundle().value != null) {
        val newStix = new IndicatorForm()
        indicatorList.append(newStix)
        controller.addStixToBundle(newStix)
        indicatorListView.getSelectionModel.select(newStix)
      }
    })
  })

  deleteButton.setOnMouseClicked((_: MouseEvent) => {
    val selectedItem = indicatorListView.getSelectionModel.getSelectedItem
    if (selectedItem != null) {
      val ndx = indicatorList.indexWhere(b => b.id == selectedItem.id)
      if (ndx != -1) {
        indicatorList.remove(ndx)
        indicatorListView.getSelectionModel.clearSelection()
        bundleController.map(_.removeStixFromBundle(selectedItem))
      }
    }
  })

  // for use in indicatorListView, to allow editing (double click) of the list entries text, the indicator name
  val indicatorStringConverter = new StringConverter[IndicatorForm] {

    def fromString(newName: String): IndicatorForm = {
      val selectedStix = indicatorListView.getSelectionModel.getSelectedItem
      if (selectedStix != null) {
        // remove the old stix from the bundleController
        bundleController.map(_.removeStixFromBundle(selectedStix))
        // put the new name in the selection
        selectedStix.name.value = newName
        // add the new selection to the bundleController
        bundleController.map(_.addStixToBundle(selectedStix))
      }
      selectedStix
    }

    def toString(stix: IndicatorForm): String = stix.name.value
  }

}