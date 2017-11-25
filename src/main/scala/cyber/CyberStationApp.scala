package cyber

import java.io.IOException
import java.security.Security
import javafx.{fxml => jfxf, scene => jfxs}

import controllers.CyberStationControllerInterface
import db.MongoDbService
import taxii.TaxiiConnection

import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafxml.core.{DependenciesByType, FXMLLoader, FXMLView, NoDependencyResolver}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.language.{implicitConversions, postfixOps}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scalafx.scene.paint.Color


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

  showThis("Trying to connect to database: " + MongoDbService.mongoUri, Color.Black)
  spinThis(true)

  // try to connect to the mongo db
  Future(try {
    // start a db connection
    MongoDbService.init()
    // wait here for the connection to complete
    Await.result(MongoDbService.database, 20 seconds)
    // load the data
    MongoDbService.loadCyberBundles().onComplete {
      case Success(theList) =>
        showThis("Connected to database: " + MongoDbService.mongoUri, Color.Black)
        controller.setBundles(theList)
      case Failure(err) =>
        showThis("Fail to load data from database: " + MongoDbService.mongoUri, Color.Red)
        println("---> bundles loading failure: " + err)
    }
    spinThis(false)
    hasDB = true
  } catch {
    case ex: Throwable =>
      showThis("Fail to connect to database: " + MongoDbService.mongoUri + " --> data will not be saved", Color.Red)
      spinThis(false)
      hasDB = false
  })

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

  private def showThis(text: String, color: Color) = Platform.runLater(() => {
    controller.messageBar().setTextFill(color)
    controller.messageBar().setText(text)
  })

  private def spinThis(onof: Boolean) = Platform.runLater(() => {controller.messageBarSpin().setVisible(onof)})

  // close properly before exiting
  override def stopApp(): Unit = {
    if (hasDB) {
      // save the data and close
      stopAppWithDB()
    }
    else {
      // just close
      TaxiiConnection.closeSystem()
      super.stopApp
      System.exit(0)
    }
  }

}
