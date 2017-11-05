package cyber

import java.io.IOException
import javafx.{fxml => jfxf, scene => jfxs}
import controllers.CyberStationControllerInterface

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.{Parent, Scene}
import scalafxml.core.{DependenciesByType, FXMLLoader, FXMLView, NoDependencyResolver}


/**
  * CyberStation Application
  *
  * @author Ringo Wathelet
  */
object CyberStationApp extends JFXApp {

  val resource = getClass.getResource("../ui/mainView.fxml")
  if (resource == null) {
    throw new IOException("Cannot load resource: mainView.fxml")
  }

 // val root = FXMLView(resource, new DependenciesByType(Map.empty))

  val loader = new FXMLLoader(resource, new DependenciesByType(Map.empty))
  loader.load()
  val root: jfxs.Parent = loader.getRoot[jfxs.Parent]
  val controller = loader.getController[CyberStationControllerInterface]
//  controller.setBusInfoServer(busInfoServer)

  stage = new PrimaryStage() {
    title = "CyberStation 0.1"
    scene = new Scene(root)
  }

}