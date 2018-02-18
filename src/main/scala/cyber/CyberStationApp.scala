package cyber

import java.io.IOException
import java.security.Security
import javafx.{fxml => jfxf, scene => jfxs}
import controllers.CyberStationControllerInterface
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafxml.core.{DependenciesByType, FXMLLoader, FXMLView, NoDependencyResolver}
import scala.language.{implicitConversions, postfixOps}


/**
  * CyberStation Application
  *
  * @author Ringo Wathelet
  */
object CyberStationApp extends JFXApp {

  val version = "0.1"

  // needed for (SSL) TLS-1.2 in https, requires jdk1.8.0_152
  Security.setProperty("crypto.policy", "unlimited")

  // create the application
  val resource = getClass.getResource("ui/mainView.fxml")
  if (resource == null) {
    throw new IOException("Cannot load resource: ui/mainView.fxml")
  }
  // val root = FXMLView(resource, new DependenciesByType(Map.empty))
  val loader = new FXMLLoader(resource, new DependenciesByType(Map.empty))
  loader.load()
  val root: jfxs.Parent = loader.getRoot[jfxs.Parent]
  val controller = loader.getController[CyberStationControllerInterface]
  stage = new PrimaryStage() {
    title = "CyberStation-" + version
    scene = new Scene(root)
  }

  // initialise the main controller
  controller.init()

  // close properly before exiting
  override def stopApp(): Unit = {
    super.stopApp
    controller.stopApp()
  }

}
