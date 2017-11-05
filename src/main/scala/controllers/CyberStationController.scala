
package controllers

import javafx.fxml.FXML
import com.jfoenix.controls.JFXTabPane
import scalafx.scene.control._
import scalafx.scene.layout.{HBox, VBox}
import scalafxml.core.macros.{nested, sfxml}


trait CyberStationControllerInterface {

}

@sfxml
class CyberStationController(mainMenu: VBox,
                             loginButton: Button,
                             messageLabel: Label,
                             serversView: HBox,
                             @FXML stixView: JFXTabPane,
                             @nested[MainMenuController] mainMenuController: MainMenuControllerInterface,
                             @nested[ServersViewController] serversViewController: ServersViewControllerInterface,
                             @nested[StixViewController] stixViewController: StixViewControllerInterface)
  extends CyberStationControllerInterface {

  // give the server info properties to the stixViewController
  stixViewController.setSelectedServer(serversViewController.serverInfo)
  stixViewController.setSelectedApiroot(serversViewController.apirootInfo)
  stixViewController.setSelectedCollection(serversViewController.collectionInfo)

}