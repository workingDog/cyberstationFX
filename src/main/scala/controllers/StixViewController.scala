package controllers

import scalafxml.core.macros.{nested, sfxml}
import scalafx.beans.property.StringProperty
import scalafx.scene.layout.HBox


trait StixViewControllerInterface {
  def init(): Unit
  def setSelectedServer(theSelectedServer: StringProperty): Unit
  def setSelectedApiroot(theSelectedApiroot: StringProperty): Unit
  def setSelectedCollection(theSelectedCollection: StringProperty): Unit
}

@sfxml
class StixViewController(bundleHBox: HBox,
                         @nested[BundleViewController] bundleViewController: BundleViewControllerInterface,
                         @nested[IndicatorController] indicatorController: IndicatorControllerInterface)
  extends StixViewControllerInterface {

  init()

  override def init() {
    indicatorController.setBundleViewController(bundleViewController)
  }

  override def setSelectedServer(theSelectedServer: StringProperty) {
    bundleViewController.setSelectedServer(theSelectedServer)
    theSelectedServer.onChange { (_, oldValue, newValue) =>
    }

  }

  override def setSelectedApiroot(theSelectedApiroot: StringProperty) {
    bundleViewController.setSelectedApiroot(theSelectedApiroot)
    theSelectedApiroot.onChange { (_, oldValue, newValue) =>
    }
  }

  override def setSelectedCollection(theSelectedCollection: StringProperty) {
    bundleViewController.setSelectedCollection(theSelectedCollection)
    theSelectedCollection.onChange { (_, oldValue, newValue) =>
    }
  }

}
