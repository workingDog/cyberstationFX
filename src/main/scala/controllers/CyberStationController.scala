
package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.JFXTabPane
import com.kodekutters.stix.Bundle
import cyber.CyberBundle

import scalafx.scene.control._
import scalafx.scene.layout.{HBox, VBox}
import scalafxml.core.macros.{nested, sfxml}


trait CyberStationControllerInterface {
  def getAllBundles(): List[CyberBundle]
  def setBundles(bundleList: List[CyberBundle])
}

@sfxml
class CyberStationController(mainMenu: VBox,
                             loginButton: Button,
                             messageLabel: Label,
                             serversView: HBox,
                             objectsView: VBox,
                             @FXML stixView: JFXTabPane,
                             @nested[ObjectsViewController] objectsViewController: ObjectsViewControllerInterface,
                             @nested[MainMenuController] mainMenuController: MainMenuControllerInterface,
                             @nested[ServersViewController] serversViewController: ServersViewControllerInterface,
                             @nested[StixViewController] stixViewController: StixViewControllerInterface)
  extends CyberStationControllerInterface {

  // give the server info properties to the stixViewController
  stixViewController.setSelectedServer(serversViewController.serverInfo)
  stixViewController.setSelectedApiroot(serversViewController.apirootInfo)
  stixViewController.setSelectedCollection(serversViewController.collectionInfo)

  // give the server info properties to the ObjectsViewController
  objectsViewController.setSelectedApiroot(serversViewController.apirootInfo)
  objectsViewController.setSelectedCollection(serversViewController.collectionInfo)

  override def getAllBundles() = stixViewController.getBundleController().getAllBundles()

  override def setBundles(bundleList: List[CyberBundle]): Unit =
    stixViewController.getBundleController().setBundles(bundleList)
}