package controllers

import java.io.File

import com.kodekutters.stix.Bundle
import com.kodekutters.taxii.{Collection, TaxiiCollection, TaxiiConnection}
import com.typesafe.config.{Config, ConfigFactory}
import cyber.ServerForm
import play.api.libs.json.Json

import scala.concurrent.Await
import scalafx.Includes._
import scalafxml.core.macros.{nested, sfxml}
import scalafx.scene.web.WebView
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._



trait TaxiiWebViewControllerInterface {
  def init(): Unit

  def doLoadAndClick(): Unit

  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit
}

@sfxml
class TaxiiWebViewController(taxiiWebViewer: WebView) extends TaxiiWebViewControllerInterface {

  var serverInfo: ServerForm = _
  var taxiiCol: TaxiiCollection = _
  var apirootInfo = ""

  val config: Config = ConfigFactory.load
  private var fetchNumber = 100

  val thePath = new java.io.File(".").getCanonicalPath + "/cti-stix-visualization/index.html"
  val file = new File(thePath)
  var theBundle: Bundle = _

  init()

  def init(): Unit = {
    try {
      fetchNumber = config.getInt("taxii.objects")
    } catch {
      case e: Throwable => println("---> config taxii.objects error: " + e)
    }
    taxiiWebViewer.setVisible(true)
    taxiiWebViewer.getEngine.javaScriptEnabled = true
    taxiiWebViewer.cache = false
    taxiiWebViewer.getEngine.load(file.toURI.toURL.toString)
  }

  def showObjects() = {
    println("----------> taxiiWebViewer " + theBundle.objects.length)
    val theJsonString = Json.stringify(Json.toJson(theBundle))
    val doc = taxiiWebViewer.getEngine.getDocument
    if (doc != null) {
      doc.getElementById("bundle-data").setAttribute("value", theJsonString)
      taxiiWebViewer.getEngine.executeScript("document.getElementById(\"bundle-data\").click();")
    }
  }

  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit = {
    cyberStationController.getSelectedServer().onChange { (_, oldValue, newValue) =>
      if (newValue != null) serverInfo = newValue
    }
    cyberStationController.getSelectedApiroot().onChange { (_, oldValue, newValue) =>
      apirootInfo = newValue
    }
    cyberStationController.getSelectedCollection().onChange { (_, oldValue, newValue) =>
      taxiiCol = newValue
    }
  }

  def doLoadAndClick(): Unit = {
    if (taxiiCol == null) return
    if (taxiiCol.id != null && apirootInfo != null) {
      val col = Collection(taxiiCol, apirootInfo, new TaxiiConnection(serverInfo.url.value,
        serverInfo.user.value, serverInfo.psw.value, 10))
      // need to wait here because want to be on the JavaFX thread to show the objects
      Await.result(
        col.getObjects(range = ("0-" + fetchNumber.toString)).map(bndl => {
          bndl.map(bundle =>
            if (theBundle != bundle && bundle != null) {
              theBundle = bundle
              col.conn.close()
            }
          )
        }), 30 second)
      showObjects()
    }
  }

}
