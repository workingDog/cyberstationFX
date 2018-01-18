package controllers

import com.kodekutters.stix.{Bundle, StixObj}

import scalafxml.core.macros.{nested, sfxml}
import scala.collection.mutable.ListBuffer
import scalafx.scene.layout.Pane
import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXRadioButton, JFXSpinner}
import com.kodekutters.taxii.{Collection, TaxiiCollection, TaxiiConnection}
import cyber.ServerForm
import graph.JungGraphMaker

import scalafx.scene.control.ToggleGroup
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalafx.application.Platform


trait GraphViewControllerInterface {

  def doLoadAndClick(stixList: ListBuffer[StixObj], text: String): Unit

  def setCyberStationController(cyberStationController: CyberStationControllerInterface): Unit
}

@sfxml
class GraphViewController(thePane: Pane,
                          graphGroup: ToggleGroup,
                          @FXML stopAnimation: JFXButton,
                          @FXML theSpinner: JFXSpinner,
                          @FXML allBundlesObj: JFXRadioButton,
                          @FXML bundleObj: JFXRadioButton,
                          @FXML taxiiObj: JFXRadioButton) extends GraphViewControllerInterface {

  var theGraphMaker: JungGraphMaker = _
  var cyberController: CyberStationControllerInterface = _
  var theStixList: ListBuffer[StixObj] = _
  var theText: String = "no data"
  var serverInfo: ServerForm = _
  var taxiiCol: TaxiiCollection = _
  var apirootInfo = ""
  var hasChanged: Boolean = false
  var theBundle: Bundle = _

  def showSpinner(onof: Boolean) = Platform.runLater(() => {
    theSpinner.setVisible(onof)
  })

  def allBundlesObjAction(): Unit = {
    // for testing
    theSpinner.setVisible(true)
    if (theStixList != null) {
      theSpinner.setVisible(true)
      JungGraphMaker(thePane).render(theStixList.toList)
    } else {
      thePane.getChildren.clear()
    }
    theSpinner.setVisible(false)

    //    if (cyberController != null) {
    //      theSpinner.setVisible(true)
    //      Platform.runLater(() => {
    //        val allCyberObj = for (bndl <- cyberController.getAllBundles().toList) yield bndl.objects.toList
    //        val allStix = for (cyberStix <- allCyberObj.flatten) yield cyberStix.toStix
    //        viewer.renderOn(thePane, allStix)
    //        showSpinner(false)
    //      })
    //    }
  }

  def bundleObjAction(): Unit = {
    if (cyberController != null) {
      theSpinner.setVisible(true)
      val cyberBundle = cyberController.getStixViewController().getBundleController().getCurrentBundle()
      if (cyberBundle != null && cyberBundle.value != null) {
        val allStix = for (cyberStix <- cyberBundle.value.objects) yield cyberStix.toStix
        JungGraphMaker(thePane).render(allStix.toList)
      } else {
        thePane.getChildren.clear()
      }
      theSpinner.setVisible(false)
    }
  }

  def taxiiObjAction(): Unit = {
    if (taxiiCol == null) return
    if (taxiiCol.id != null && apirootInfo != null) {
      if (hasChanged) {
        hasChanged = false
        theSpinner.setVisible(true)
        val col = Collection(taxiiCol, apirootInfo, new TaxiiConnection(serverInfo.url.value,
          serverInfo.user.value, serverInfo.psw.value, 10))
        // need to wait here because want to be on the JavaFX thread to show the objects
        //  range = ("0-" + fetchNumber.toString)
        Await.result(
          col.getObjects().map(bndl => {
            bndl.map(bundle =>
              if (theBundle != bundle && bundle != null) {
                theBundle = bundle
                col.conn.close()
              }
            )
          }), 30 second)
        JungGraphMaker(thePane).render(theBundle.objects.toList)
        theSpinner.setVisible(false)
      } else {
        JungGraphMaker(thePane).render(theBundle.objects.toList)
      }
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

  def doLoadAndClick(stixList: ListBuffer[StixObj], text: String): Unit = {
    allBundlesObj.setSelected(true)
    if (theStixList != stixList && stixList != null) {
      theStixList = stixList
      theText = text
      theSpinner.setVisible(true)
      theGraphMaker = JungGraphMaker(thePane)
      theGraphMaker.render(stixList.toList)
      theSpinner.setVisible(false)
    }
  }

  def stopAnimationAction() = if (theGraphMaker != null) theGraphMaker.stopAnimation()


}
