package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXSpinner, JFXTabPane}
import cyber.CyberBundle
import taxii.TaxiiCollection

import scalafx.beans.property.{StringProperty, ObjectProperty}
import scalafx.scene.control._
import scalafx.scene.layout.{HBox, VBox}
import scalafxml.core.macros.{nested, sfxml}


trait CyberStationControllerInterface {
  def getAllBundles(): List[CyberBundle]

  def setBundles(bundleList: List[CyberBundle])

  def messageBar(): Label

  def messageBarSpin(): JFXSpinner

  def getSelectedServer(): StringProperty

  def getSelectedApiroot(): StringProperty

  def getSelectedCollection(): ObjectProperty[TaxiiCollection]
}

@sfxml
class CyberStationController(mainMenu: VBox,
                             loginButton: Button,
                             messageLabel: Label,
                             serversView: HBox,
                             objectsView: VBox,
                             @FXML msgBarSpinner: JFXSpinner,
                             @FXML stixView: JFXTabPane,
                             @nested[ObjectsViewController] objectsViewController: ObjectsViewControllerInterface,
                             @nested[MainMenuController] mainMenuController: MainMenuControllerInterface,
                             @nested[ServersViewController] serversViewController: ServersViewControllerInterface,
                             @nested[StixViewController] stixViewController: StixViewControllerInterface)
  extends CyberStationControllerInterface {

  override def getSelectedServer() = serversViewController.serverInfo

  override def getSelectedApiroot() = serversViewController.apirootInfo

  override def getSelectedCollection() = serversViewController.collectionInfo

  // give this controller to the stixViewController
  stixViewController.setCyberStationController(this)

  // give this controller to the ObjectsViewController
  objectsViewController.setCyberStationController(this)

  override def getAllBundles() = stixViewController.getBundleController().getAllBundles()

  override def setBundles(bundleList: List[CyberBundle]): Unit =
    stixViewController.getBundleController().setBundles(bundleList)

  override def messageBar(): Label = messageLabel

  override def messageBarSpin(): JFXSpinner = msgBarSpinner
}