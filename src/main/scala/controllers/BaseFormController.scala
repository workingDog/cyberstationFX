package controllers

import com.jfoenix.controls.JFXListView
import cyber.CyberObj

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.Label
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.util.StringConverter


trait BaseControllerInterface {
  def setBundleViewController(controller: BundleViewControllerInterface): Unit
}

trait BaseSpecControllerInterface {
  def control(stix: CyberObj, controller: Option[BundleViewControllerInterface]): Unit

  def clear(): Unit
}


class BaseFormController(cyberType: String,
                         formListView: JFXListView[CyberObj],
                         bundleLabel: Label,
                         commonController: CommonControllerInterface,
                         specController: BaseSpecControllerInterface) {

  val formList = ObservableBuffer[CyberObj]()
  var bundleController: Option[BundleViewControllerInterface] = None

  init()

  def init(): Unit = {
    // setup the list of CyberObj
    formListView.setEditable(true)
    formListView.setExpanded(true)
    formListView.setDepth(1)
    formListView.setItems(formList)
    // to edit the name in the list by double clicking on it
    formListView.cellFactory = { _ =>
      new TextFieldListCell[CyberObj] {
        converter = formStringConverter
        item.onChange { (_, _, obj) => if (obj != null) text = obj.name.value }
      }
    }
    formListView.getSelectionModel.selectedItem.onChange { (_, oldValue, newValue) =>
      // the commonController will take care of all interactions/updates for the common attributes
      if (newValue != null) {
        commonController.control(newValue, bundleController)
        specController.control(newValue, bundleController)
      } else {
        commonController.clear()
        specController.clear()
      }
    }
  }

  def setBundleViewController(controller: BundleViewControllerInterface): Unit = {
    bundleController = Option(controller)
    // this is to clear the list of CyberObj when the list of bundles is cleared
    bundleController.map(controller => {
      controller.getAllBundles().onChange((source, changes) => {
        if (controller.getAllBundles().isEmpty) formList.clear()
      })
    })
    // get the BundleViewController currentBundle
    val currentBundle = controller.getCurrentBundle()
    val bndlName = if (currentBundle.value != null) currentBundle.value.name.value else ""
    bundleLabel.text = "Part of bundle: " + bndlName
    if (currentBundle != null) {
      currentBundle.onChange { (source, oldValue, newValue) =>
        // load the new bundle list of CyberObj
        if (newValue != null) {
          val objForms = newValue.objects.filter(stix => stix.`type`.value == cyberType)
          formList.clear()
          formList.appendAll(objForms)
          bundleLabel.text = "Part of bundle: " + newValue.name.value
        } else {
          bundleLabel.text = "Part of bundle: "
        }
      }
    }
  }

  def doDelete() = {
    val selectedItem = formListView.getSelectionModel.getSelectedItem
    if (selectedItem != null) {
      val ndx = formList.indexWhere(b => b.id == selectedItem.id)
      if (ndx != -1) {
        formList.remove(ndx)
        formListView.getSelectionModel.clearSelection()
        bundleController.map(_.removeStixFromBundle(selectedItem))
      }
    }
  }

  def doAdd(newStix: CyberObj) = {
    bundleController.map(controller => {
      if (controller.getCurrentBundle().value != null) {
        formList.append(newStix)
        controller.addStixToBundle(newStix)
        formListView.getSelectionModel.select(newStix)
      }
    })
  }

  // for use in formListView, to allow editing (double click) of the list entries text, the CyberObj name
  val formStringConverter = new StringConverter[CyberObj] {

    def fromString(newName: String): CyberObj = {
      val selectedStix = formListView.getSelectionModel.getSelectedItem
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

    def toString(stix: CyberObj): String = stix.name.value
  }

}
