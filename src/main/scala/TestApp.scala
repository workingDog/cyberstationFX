
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ListView, SelectionMode}
import scalafx.scene.Scene
import scalafx.Includes._

object TestApp extends JFXApp {
  val labels = ObservableBuffer[String]("one", "two", "three", "four")
  val myListView = new ListView[String](labels)
  myListView.getSelectionModel.selectionMode = SelectionMode.Multiple
  myListView.getSelectionModel.selectedItems.onChange { (oldList, newList) =>
    println("--> selections: " + myListView.getSelectionModel.getSelectedItems)
  }
  stage = new PrimaryStage {
    title = "ListViewTest"
    scene = new Scene {
      root = myListView
    }
  }
}



  //    val newStix = new IndicatorForm()
  //    val cyber = new CyberBundle("xxxx", newStix)

  //        val cyberjson = Json.toJson(cyber)
  //        println("--> cyberjson=" + Json.stringify(cyberjson))
  //
  //        val bndl = cyber.toStix
  //        val bndljson = Json.toJson(bndl)
  //        println("--> bndljson =" + Json.stringify(bndljson))

  //    if (!Utils.urlValid("https://")) {
  //      println("-----> URL NOT valid")
  //    } else {
  //      println("-----> URL IS valid")
  //    }

  //    var newStix = new Indicator(name = Some("indicator"), pattern = "", valid_from = Timestamp.now())
  //    println("--> before newStix=" + newStix.toString)
  //    newStix = newStix.copy(lang = Option("bbbbbbbbbbb"))
  //    println("--> after newStix=" + newStix.toString)

  //  }


