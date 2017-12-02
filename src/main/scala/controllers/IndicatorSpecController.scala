package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXTextField}
import com.kodekutters.stix.Timestamp
import cyber.IndicatorForm

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.scene.input.MouseEvent
import scalafxml.core.macros.sfxml


trait IndicatorSpecControllerInterface {
  def control(stix: IndicatorForm, controller: Option[BundleViewControllerInterface]): Unit

  def clear(): Unit
}

@sfxml
class IndicatorSpecController(@FXML patternField: JFXTextField,
                              @FXML validFromButton: JFXButton,
                              @FXML validFromField: JFXTextField,
                              @FXML validUntilButton: JFXButton,
                              @FXML validUntilField: JFXTextField,
                             ) extends IndicatorSpecControllerInterface {

  var currentForm: IndicatorForm = null

  init()

  def init(): Unit = {
    validFromButton.setOnMouseClicked((_: MouseEvent) => {
      validFromField.setText(Timestamp.now().toString())
    })
    validUntilButton.setOnMouseClicked((_: MouseEvent) => {
      validUntilField.setText(Timestamp.now().toString())
    })

  }

  private def loadValues(): Unit = {
    validFromField.setText(currentForm.valid_from.value)
    validUntilField.setText(currentForm.valid_until.value)
    patternField.setText(currentForm.pattern.value)
  }

  def clear(): Unit = {
    unbindAll()
    validFromField.setText("")
    validUntilField.setText("")
    patternField.setText("")
  }

  private def unbindAll(): Unit = {
    if (currentForm != null) {
      currentForm.valid_from.unbind()
      currentForm.valid_until.unbind()
      currentForm.pattern.unbind()
      currentForm = null
    }
  }

  def control(stix: IndicatorForm, controller: Option[BundleViewControllerInterface]): Unit = {
    unbindAll()
    if (stix != null) {
      currentForm = stix
      loadValues()
      // bind the form to the UI
      currentForm.valid_from <== validFromField.textProperty()
      currentForm.valid_until <== validUntilField.textProperty()
      currentForm.pattern <== patternField.textProperty()
    }
  }

}