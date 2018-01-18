package controllers

import java.io.File
import javafx.fxml.FXML

import com.jfoenix.controls.JFXRadioButton
import com.kodekutters.stix.{Bundle, StixObj}
import com.kodekutters.taxii.{Collection, TaxiiCollection, TaxiiConnection}
import com.typesafe.config.{Config, ConfigFactory}

import scalafx.concurrent.Worker
import scalafxml.core.macros.{nested, sfxml}
import scalafx.scene.web.WebView
import cyber.{CytoObject, ServerForm}
import netscape.javascript.JSObject
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scalafx.scene.control.ToggleGroup
import scalafx.Includes._

trait WebViewControllerInterface {
  def init(): Unit

  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit

  def doLoadAndClick(stixList: ListBuffer[StixObj], text: String): Unit
}

@sfxml
class WebViewController(webViewer: WebView,
                        graphGroup: ToggleGroup,
                        @FXML allBundlesObj: JFXRadioButton,
                        @FXML bundleObj: JFXRadioButton,
                        @FXML taxiiObj: JFXRadioButton) extends WebViewControllerInterface {

  var cyberController: CyberStationControllerInterface = _
  var theText: String = "no data"
  var theStixList: ListBuffer[StixObj] = _
  var serverInfo: ServerForm = _
  var taxiiCol: TaxiiCollection = _
  var apirootInfo = ""
  val config: Config = ConfigFactory.load
  var fetchNumber = 100
  var hasChanged: Boolean = false
  //   cti-stix-visualization   cytoscape
  val indexURI = new File(new java.io.File(".").getCanonicalPath + "/cti-stix-visualization/index.html").toURI.toURL.toString

  // a bridge between scala and javascript
  // when the javascript console.log() is called it will call this scala log()
  class JavaBridge {
    def log(text: String): Unit = println("===log==> " + text)
  }

  val bridge = new JavaBridge()

  init()

  def init(): Unit = {
    try {
      fetchNumber = config.getInt("taxii.objects")
    } catch {
      case e: Throwable => println("---> config taxii.objects error: " + e)
    }
    val manager = new java.net.CookieManager()
    java.net.CookieHandler.setDefault(manager)
    manager.getCookieStore.removeAll
    webViewer.setVisible(true)
    webViewer.getEngine.javaScriptEnabled = true
    webViewer.cache = false
    whenReady()
  }

  def whenReady(): Unit = {
    webViewer.getEngine.getLoadWorker.stateProperty.onChange { (_, _, newValue) =>
      if (newValue.eq(Worker.State.Succeeded.delegate) && theStixList != null) {
        val window = webViewer.getEngine.executeScript("window").asInstanceOf[JSObject]
        window.setMember("java", bridge)
        webViewer.getEngine.executeScript("console.log = function(message)\n" + "{\n" + "    java.log(message);\n" + "};")
        doLoad()
      }
    }
  }

  def doLoad() = {
    //val theJsonString = CytoObject.toCytoEls(theStixList)
    val theJsonString = Json.stringify(Json.toJson(Bundle(theStixList)))
    val doc = webViewer.getEngine.getDocument
    if (doc != null) {
      doc.getElementById("titleString").setTextContent(theText)
      doc.getElementById("bundle-data").setAttribute("value", theJsonString)
      webViewer.getEngine.executeScript("document.getElementById(\"bundle-data\").click();")
    }
  }

  def doLoadAndClick(stixList: ListBuffer[StixObj], text: String): Unit = {
    theText = "File: " + text
    allBundlesObj.setSelected(true)
    if (theStixList != stixList && stixList != null) {
      theStixList = stixList
      webViewer.getEngine.load(indexURI)
    }
  }

  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit = {
    cyberController = cyberStationController
    cyberStationController.getSelectedServer().onChange { (_, oldValue, newValue) =>
      if (newValue != null) serverInfo = newValue
      hasChanged = true
    }
    cyberStationController.getSelectedApiroot().onChange { (_, oldValue, newValue) =>
      apirootInfo = newValue
      hasChanged = true
    }
    cyberStationController.getSelectedCollection().onChange { (_, oldValue, newValue) =>
      taxiiCol = newValue
      hasChanged = true
    }
  }

  def allBundlesObjAction(): Unit = {
    theText = "All Bundles ---> not yet implemented"
    webViewer.getEngine.load(indexURI)
    //    theText = ""
    //    if (cyberController != null) {
    //      val allCyberObj = for (bndl <- cyberController.getAllBundles().toList) yield bndl.objects.toList
    //      val allStix = for (cyberStix <- allCyberObj.flatten) yield cyberStix.toStix
    //      theText = "All Bundles"
    //      if (theStixList.toList != allStix && allStix != null) {
    //        theStixList = allStix.to[ListBuffer]
    //        webViewer.getEngine.load(indexURI)
    //      }
    //    }
  }

  def bundleObjAction(): Unit = {
    theText = ""
    if (cyberController != null) {
      val cyberBundle = cyberController.getStixViewController().getBundleController().getCurrentBundle()
      if (cyberBundle != null && cyberBundle.value != null) {
        theText = cyberBundle.value.name.value
        val allStix = for (cyberStix <- cyberBundle.value.objects) yield cyberStix.toStix
        if (theStixList.toList != allStix.toList && allStix != null) {
          theStixList = allStix.toList.to[ListBuffer]
          webViewer.getEngine.load(indexURI)
        }
      }
    }
  }

  def taxiiObjAction(): Unit = {
    theText = ""
    if (taxiiCol == null) return
    if (taxiiCol.id != null && apirootInfo != null) {
    //  if (hasChanged) {
    //    hasChanged = false
        val col = Collection(taxiiCol, apirootInfo, new TaxiiConnection(serverInfo.url.value,
          serverInfo.user.value, serverInfo.psw.value, 10))
        theText = taxiiCol.title
        // need to wait here because want to be on the JavaFX thread to show the objects
        //  range = ("0-" + fetchNumber.toString)
        Await.result(
          col.getObjects().map(bndl => {
            bndl.map(bundle =>
              if (theStixList != bundle.objects) {
                theStixList = bundle.objects.toList.to[ListBuffer]
                col.conn.close()
              }
            )
          }), 60 second)
        webViewer.getEngine.load(indexURI)
      }
  //  }
  }


}
