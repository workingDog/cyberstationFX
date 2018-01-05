package controllers

import java.io.File
import javafx.beans.value.ChangeListener
import javafx.concurrent.Worker.State
import javafx.fxml.FXML

import com.jfoenix.controls.JFXButton
import com.kodekutters.stix.Bundle
import play.api.libs.json.Json

import scalafx.Includes._
import scalafx.concurrent.Worker
import scalafxml.core.macros.{nested, sfxml}
import scalafx.scene.web.WebView


trait WebViewControllerInterface {
  def init(): Unit

  def doLoadAndClick(bundle: Bundle): Unit
}

@sfxml
class WebViewController(webViewer: WebView) extends WebViewControllerInterface {

  //  val scriptPath = new java.io.File(".").getCanonicalPath + "/cti-stix-visualization/application.js"
  val thePath = new java.io.File(".").getCanonicalPath + "/cti-stix-visualization/index.html"
  // val neo4jUrl = thePath //  "http://localhost:7474/browser/" // 7687  7474
  val file = new File(thePath)
  var jsString: String = ""
  var theBundle: Bundle = _

  init()

  def init(): Unit = {
    webViewer.setVisible(true)
    webViewer.getEngine.javaScriptEnabled = true
    webViewer.cache = false
    //  whenReady()
    webViewer.getEngine.load(file.toURI.toURL.toString)
  }

  def doLoadAndClick(bundle: Bundle): Unit = {
    if (theBundle != bundle && bundle != null) {
      theBundle = bundle
      println("----------> webViewer doLoadAndClick " + theBundle.objects.length)
      //    webViewer.getEngine.reload()
      val theJsonString = Json.stringify(Json.toJson(theBundle))
      val doc = webViewer.getEngine.getDocument
      if (doc != null) {
        doc.getElementById("bundle-data").setAttribute("value", theJsonString)
        webViewer.getEngine.executeScript("document.getElementById(\"bundle-data\").click();")
      }
    }
  }

  def whenReady(): Unit = {
    webViewer.getEngine.getLoadWorker.stateProperty.addListener(new ChangeListener[State] {
      def changed(observable: javafx.beans.value.ObservableValue[_ <: State], oldValue: State, newValue: State) {
        if (newValue.eq(Worker.State.Succeeded.delegate) && theBundle != null) {
          val theJsonString = Json.stringify(Json.toJson(theBundle))
          val doc = webViewer.getEngine.getDocument
          if (doc != null) {
            println("----------> whenReady new bundle")
            doc.getElementById("bundle-data").setAttribute("value", theJsonString)
            webViewer.getEngine.executeScript("document.getElementById(\"bundle-data\").click();")
          }
        }
      }
    })
  }

  def refreshView(): Unit = {

  }


  def testScript(): Unit = {
    //  webViewer.engine.executeScript("handleTextarea()")
    //  webViewer.engine.executeScript(scriptPath + "/" + s"vizStixWrapper($testJson)")
  }

  val testJson =
    """{"type": "bundle","id":
     "bundle--9f0725cb-4bc3-47c3-aba6-99cb97ba4f52",
     "spec_version": "2.0","objects": [
        {
          "type": "malware",
          "id": "malware--efd5ac80-79ba-45cc-9293-01460ad85303",
          "created": "2017-07-18T22:00:30.405Z",
          "modified": "2017-07-18T22:00:30.405Z",
          "name": "IMDDOS",
          "labels": [
            "bot",
            "ddos"
          ],
          "description": "Once infected with this malware, a host becomes part of the IMDDOS Botnet",
          "kill_chain_phases": [
            {
              "kill_chain_name": "lockheed-martin-cyber-kill-chain",
              "phase_name": "exploit"
            }
          ]
        }]}""".stripMargin


}
