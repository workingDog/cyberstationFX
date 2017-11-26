package controllers

import scalafxml.core.macros.{nested, sfxml}
import scalafx.scene.layout.HBox


trait StixViewControllerInterface {
  def init(): Unit

  def getBundleController(): BundleViewControllerInterface

  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit
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

  override def getBundleController(): BundleViewControllerInterface = bundleViewController

  override def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit = {
    bundleViewController.setCyberStationController(cyberStationController)
  }

}
