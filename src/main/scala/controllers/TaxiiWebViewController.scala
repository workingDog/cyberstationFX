//package controllers
//
//import java.io.File
//import javafx.beans.value.ChangeListener
//import javafx.concurrent.Worker.State
//
//import com.kodekutters.stix.Bundle
//import com.kodekutters.taxii.{Collection, TaxiiCollection, TaxiiConnection}
//import com.typesafe.config.{Config, ConfigFactory}
//import cyber.{CytoObject, ServerForm}
//import org.w3c.dom.Document
//import org.w3c.dom.html.{HTMLElement, HTMLInputElement}
//import play.api.libs.json.Json
//
//import scala.concurrent.Await
//import scalafx.Includes._
//import scalafxml.core.macros.{nested, sfxml}
//import scalafx.scene.web.WebView
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.duration._
//import scalafx.application.Platform
//import scalafx.concurrent.Worker
//
//
//trait TaxiiWebViewControllerInterface {
//  def init(): Unit
//
//  def doLoadAndClick(): Unit
//
//  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit
//}
//
//@sfxml
//class TaxiiWebViewController(taxiiWebViewer: WebView) extends TaxiiWebViewControllerInterface {
//
//  var serverInfo: ServerForm = _
//  var taxiiCol: TaxiiCollection = _
//  var apirootInfo = ""
//
//  val config: Config = ConfigFactory.load
//  private var fetchNumber = 100
//
//  val thePath = new java.io.File(".").getCanonicalPath + "/cytoscape/index.html"
//  val file = new File(thePath)
//  var theBundle: Bundle = _
//
//  init()
//
//  def init(): Unit = {
//    val manager = new java.net.CookieManager()
//    java.net.CookieHandler.setDefault(manager)
//    manager.getCookieStore.removeAll
//    try {
//      fetchNumber = config.getInt("taxii.objects")
//    } catch {
//      case e: Throwable => println("---> config taxii.objects error: " + e)
//    }
//    taxiiWebViewer.setVisible(true)
//    taxiiWebViewer.getEngine.javaScriptEnabled = true
//    taxiiWebViewer.cache = false
//  //  whenReady()
//    taxiiWebViewer.getEngine.load(file.toURI.toURL.toString)
//  //  taxiiWebViewer.getEngine.loadContent(thePage)
//  }
//
//  def whenReady(): Unit = {
//    taxiiWebViewer.getEngine.getLoadWorker.stateProperty.addListener(new ChangeListener[State] {
//      def changed(observable: javafx.beans.value.ObservableValue[_ <: State], oldValue: State, newValue: State) {
//        if (newValue.eq(Worker.State.Succeeded.delegate) && theBundle != null) {
//          showObjects()
//        }
//      }
//    })
//  }
//
//  def showObjects() = {
//    val theJsonString = CytoObject.toCytoEls(theBundle)
//  //  val theJsonString = Json.stringify(Json.toJson(theBundle))
//    val doc = taxiiWebViewer.getEngine.getDocument
//    if (doc != null) {
//      doc.getElementById("titleString").setTextContent("Selected server endpoint")
//      doc.getElementById("bundle-data").setAttribute("value", theJsonString)
//      taxiiWebViewer.getEngine.executeScript("document.getElementById(\"bundle-data\").click();")
//    }
//  }
//
//  def showObjects2() = {
//    //  taxiiWebViewer.getEngine.loadContent(thePage)
//    //    taxiiWebViewer.getEngine.load(file.toURI.toURL.toString)
//    val theJsonString = CytoObject.toCytoEls(theBundle)
//    //Json.stringify(Json.toJson(theBundle))
//    var doc: Document = null
//     doc = taxiiWebViewer.getEngine.getDocument
//    if (doc != null) {
//      doc.getElementById("titleString").setTextContent("The TAXII server objects")
//      doc.getElementById("bundle-data").setAttribute("value", theJsonString)
//      taxiiWebViewer.getEngine.executeScript("document.getElementById(\"bundle-data\").click();")
//    }
//  }
//
//  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit = {
//    cyberStationController.getSelectedServer().onChange { (_, oldValue, newValue) =>
//      if (newValue != null) serverInfo = newValue
//    }
//    cyberStationController.getSelectedApiroot().onChange { (_, oldValue, newValue) =>
//      apirootInfo = newValue
//    }
//    cyberStationController.getSelectedCollection().onChange { (_, oldValue, newValue) =>
//      taxiiCol = newValue
//    }
//  }
//
//  def doLoadAndClick(): Unit = {
//    if (taxiiCol == null) return
//    if (taxiiCol.id != null && apirootInfo != null) {
//      val col = Collection(taxiiCol, apirootInfo, new TaxiiConnection(serverInfo.url.value,
//        serverInfo.user.value, serverInfo.psw.value, 10))
//      // need to wait here because want to be on the JavaFX thread to show the objects
//      //  range = ("0-" + fetchNumber.toString)
//      Await.result(
//        col.getObjects().map(bndl => {
//          bndl.map(bundle =>
//            if (theBundle != bundle && bundle != null) {
//              theBundle = bundle
//              col.conn.close()
//            }
//          )
//        }), 30 second)
//      println("---> theBundle: "+ theBundle.toString)
//        showObjects()
//    }
//  }
//
//// todo ---> does not work with taxiiWebViewer.getEngine.loadContent(thePage)
//  val thePage: String =
//    """<html lang="en">
//       <head>
//           <title>stix title demo</title>
//           <meta name="viewport" content="width=device-width, user-scalable=no, initial-scale=1, maximum-scale=1">
//           <link href="style.css" rel="stylesheet"/>
//           <!-- For loading external data files
//           <script src="https://cdn.polyfill.io/v2/polyfill.min.js?features=Promise,fetch"></script> -->
//           <script src="cytoscape.min.js"></script>
//           <script src="cytoscape-cose-bilkent.js"></script>
//           <!--   <script src="https://unpkg.com/cytoscape/dist/cytoscape.min.js"></script> -->
//         </head>
//
//         <style>
//         body {
//         font-family: helvetica;
//         font-size: 14px;
//         }
//
//         #cy {
//         width: 100%;
//         height: 100%;
//         position: absolute;
//         left: 0;
//         top: 0;
//         z-index: 999;
//         }
//
//         h1 {
//         opacity: 0.5;
//         font-size: 1em;
//         }
//         </style>
//
//         <body>
//         <h1>stix demo</h1>
//         <div id="cy"></div>
//         <input type="hidden" id="bundle-data" value=""/>
//
//         <!-- Load appplication code at the end to ensure DOM is loaded -->
//       <script src="showstix.js"></script>
//
//       </body>
//       </html>""".stripMargin
//}
