package controllers

import java.io.File

import com.kodekutters.stix.{Bundle, StixObj}

import scalafx.Includes._
import scalafx.concurrent.Worker
import scalafxml.core.macros.sfxml
import scalafx.scene.web.WebView
import play.api.libs.json.Json

import scala.collection.mutable.ListBuffer



trait FileWebViewControllerInterface {
  def init(): Unit

  def doLoadAndClick(stixList: ListBuffer[StixObj], text: String): Unit
}

@sfxml
class FileWebViewController(fileViewer: WebView) extends FileWebViewControllerInterface {

  // "/cytoscape/index.html"      "/cti-stix-visualization/index.html"
  val thePath = new java.io.File(".").getCanonicalPath + "/cti-stix-visualization/index.html"
  val file = new File(thePath)
  val thePath2 = new java.io.File(".").getCanonicalPath + "/cti-stix-visualization/index1.html"
  val file2 = new File(thePath2)
  var currentFile = file
  var theStixList: ListBuffer[StixObj] = _
  var theText: String = ""

  init()

  def init(): Unit = {
    currentFile = file
    val manager = new java.net.CookieManager()
    java.net.CookieHandler.setDefault(manager)
    manager.getCookieStore.removeAll
    fileViewer.setVisible(true)
    fileViewer.setContextMenuEnabled(false)
    fileViewer.getEngine.javaScriptEnabled = true
    fileViewer.cache = false
    //whenReady()  // for cytoscape
    fileViewer.getEngine.load(currentFile.toURI.toURL.toString)
    //  fileViewer.getEngine.loadContent(thePage)  // does not work
  }

  def whenReady(): Unit = {
    fileViewer.getEngine.getLoadWorker.stateProperty().addListener((observable, oldValue, newValue) => {
      if (newValue.eq(Worker.State.Succeeded.delegate) && theStixList != null) {
        if (currentFile == file) currentFile = file2 else currentFile = file
        showObjects()
      }
    })
  }

  def doLoadAndClick(stixList: ListBuffer[StixObj], text: String): Unit = {
    if (theStixList != stixList && stixList != null) {
      theStixList = stixList
      theText = text
      showObjects()
    }
  }

  def showObjects() = {
    //  val theJsonString = CytoObject.toCytoEls(theStixList) // for cytoscape
    val theJsonString = Json.stringify(Json.toJson(Bundle(theStixList)))
    val doc = fileViewer.getEngine.getDocument
    if (doc != null) {
      doc.getElementById("titleString").setTextContent("File " + theText)
      doc.getElementById("bundle-data").setAttribute("value", theJsonString)
      fileViewer.getEngine.executeScript("document.getElementById(\"bundle-data\").click();")
    }
  }

}
