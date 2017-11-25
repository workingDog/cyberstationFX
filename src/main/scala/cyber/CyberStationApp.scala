package cyber

import java.io.IOException
import java.security.Security
import javafx.{fxml => jfxf, scene => jfxs}

import controllers.CyberStationControllerInterface
import db.MongoDbService
import taxii.TaxiiConnection

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.{Parent, Scene}
import scalafxml.core.{DependenciesByType, FXMLLoader, FXMLView, NoDependencyResolver}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.language.{implicitConversions, postfixOps}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


/**
  * CyberStation Application
  *
  * @author Ringo Wathelet
  */
object CyberStationApp extends JFXApp {

  // needed for (SSL) TLS-1.2 in https, requires jdk1.8.0_152
  Security.setProperty("crypto.policy", "unlimited")

  // determine if have a db
  var hasDB = false

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
    title = "CyberStation 0.1"
    scene = new Scene(root)
  }

  // try to connect to the mongo db
  try {
    // start a db connection
    MongoDbService.init()
    // wait here for the connection to complete
    Await.result(MongoDbService.database, 20 seconds)
    // load the data
    MongoDbService.loadCyberBundles().onComplete {
      case Success(theList) => controller.setBundles(theList)
      case Failure(err) => println("---> bundles loading failure: " + err)
    }
    hasDB = true
  } catch {
    case ex => hasDB = false
  }

  // save the data and close properly before exiting
  private def stopAppWithDB(): Unit = {
    // delete the old bundles collection
    MongoDbService.dropAllBundles()
    // save the current bundles
    MongoDbService.saveAllBundles(controller.getAllBundles()).onComplete {
      case Success(result) =>
        println("---> bundles saved")
        MongoDbService.close()
        TaxiiConnection.closeSystem()
        super.stopApp
        System.exit(0)

      case Failure(err) =>
        println("---> bundles saving failure: " + err)
        MongoDbService.close()
        TaxiiConnection.closeSystem()
        super.stopApp
        System.exit(0)
    }
  }

  // close properly before exiting
  override def stopApp(): Unit = {
    if (hasDB) {
      // save the data and close
      stopAppWithDB()
    }
    else {
      TaxiiConnection.closeSystem()
      super.stopApp
      System.exit(0)
    }
    //  println(":::press a key to stop: ")
    //  var inx = scala.io.StdIn.readLine()
  }

}
