package cyber

import java.io.IOException
import java.security.Security
import javafx.{fxml => jfxf, scene => jfxs}

import controllers.CyberStationControllerInterface
import taxii.TaxiiConnection

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

  // needed for (SSL) TLS-1.2 in https, requires jdk1.8.0_152
  Security.setProperty("crypto.policy", "unlimited")

  val resource = getClass.getResource("ui/mainView.fxml")
  if (resource == null) {
    throw new IOException("Cannot load resource: mainView.fxml")
  }

  // val root = FXMLView(resource, new DependenciesByType(Map.empty))

  val loader = new FXMLLoader(resource, new DependenciesByType(Map.empty))
  loader.load()
  val root: jfxs.Parent = loader.getRoot[jfxs.Parent]
  val controller = loader.getController[CyberStationControllerInterface]

  stage = new PrimaryStage() {
    title = "CyberStation 0.1"
    scene = new Scene(root)
  }

  // close properly before exiting
  override def stopApp(): Unit = {
    TaxiiConnection.closeSystem()
    super.stopApp
    System.exit(0)
  }

}