
package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.JFXTabPane

import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.layout.{HBox, VBox}
import scalafxml.core.macros.{nested, sfxml}


trait CyberStationControllerInterface {
  // def doLogin(event: ActionEvent): Unit
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

  // println(s"Window controller initialized with nested control $nested and controller $nestedController")
  // serversViewer.doSomething()

  //  val login = new LoginHandler(loginButton)

  //  val serversListHandler = new ServersListHandler(serverListView, serverAddButton, serverDeleteButton)

  def doSomething(event: ActionEvent) {
    println("in doSomething")
  }

  // give the server info properties to the stixViewController
  stixViewController.setSelectedServer(serversViewController.serverInfo)
  stixViewController.setSelectedApiroot(serversViewController.apirootInfo)
  stixViewController.setSelectedCollection(serversViewController.collectionInfo)


  // messageLabel.setText("a new message is here")
  //  println(s"--> in CyberStation messageLabel: $messageLabel")


}