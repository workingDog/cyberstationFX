package controllers

import db.MongoDbService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalafx.scene.control.MenuItem
import scalafx.scene.paint.Color
import scalafxml.core.macros.sfxml



trait MainMenuControllerInterface {
  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit

  def init(): Unit

  def loadAction(): Unit

  def saveAction(): Unit

  def aboutAction(): Unit

  def newAction(): Unit

  def quitAction(): Unit
}

@sfxml
class MainMenuController(loadItem: MenuItem,
                         saveItem: MenuItem,
                         quitItem: MenuItem,
                         aboutItem: MenuItem,
                         newItem: MenuItem
                        ) extends MainMenuControllerInterface {

  var cyberController: CyberStationControllerInterface = _

  override def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit = {
    cyberController = cyberStationController
  }

  override def init() {

  }

  override def loadAction() {

  }

  override def saveAction() {

  }

  override def aboutAction() {
    println("---> in aboutAction")
  }

  override def newAction() {


  }

  override def quitAction() {
    cyberController.stopApp()
  }

  private def loadCyberBundle(): Unit = {
    if (MongoDbService.hasDB) {
      cyberController.showThis("Loading bundle from database: " + MongoDbService.mongoUri, Color.Black)
      cyberController.messageBarSpin().setVisible(true)
      // try to load
      Future(try {
        MongoDbService.loadCyberBundles().onComplete {
          case Success(theList) =>
            cyberController.showThis("Bundle loaded from database: " + MongoDbService.mongoUri, Color.Black)
            cyberController.getStixViewController().getBundleController().setBundles(theList)
          case Failure(err) =>
            cyberController.showThis("Fail to load bundle from database: " + MongoDbService.mongoUri, Color.Red)
            println("---> bundles loading failure: " + err)
        }
        cyberController.messageBarSpin().setVisible(false)
      } catch {
        case ex: Throwable =>
          cyberController.showThis("Fail to connect to database: " + MongoDbService.mongoUri + " --> data not be loaded", Color.Red)
          cyberController.messageBarSpin().setVisible(false)
      })
    }
    else {
      cyberController.showThis("No database: " + MongoDbService.mongoUri + " --> data not loaded", Color.Red)
    }
  }

}
