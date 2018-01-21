package controllers

import javafx.fxml.FXML

import com.jfoenix.controls.{JFXButton, JFXListView, JFXTextArea, JFXTextField}
import com.kodekutters.stix.Relationship
import cyber._

import scala.language.implicitConversions
import scalafx.Includes._
import scalafx.scene.control.Label
import scalafx.scene.input.MouseEvent
import scalafxml.core.macros.{nested, sfxml}


/**
  * the Relationship controller
  */
trait RelationshipControllerInterface extends BaseControllerInterface

@sfxml
class RelationshipController(@FXML theListView: JFXListView[MalwareForm],
                             @FXML addButton: JFXButton,
                             @FXML deleteButton: JFXButton,
                             bundleLabel: Label,
                             @nested[CommonController] commonController: CommonControllerInterface,
                             @nested[RelationshipSpecController] relationshipSpecController: RelationshipSpecControllerInterface)
  extends RelationshipControllerInterface {

  // the base controller for the common properties
  val baseForm = new BaseFormController(Relationship.`type`, theListView.asInstanceOf[JFXListView[CyberObj]],
    bundleLabel, commonController, relationshipSpecController)

  deleteButton.setOnMouseClicked((_: MouseEvent) => baseForm.doDelete())
  addButton.setOnMouseClicked((_: MouseEvent) => baseForm.doAdd(new RelationshipForm()))

  // give the base controller the BundleViewControllerInterface, so it can refer to it
  def setBundleViewController(controller: BundleViewControllerInterface): Unit = baseForm.setBundleViewController(controller)

}

/**
  * the controller for all Relationship specific properties, i.e. other than the common ones
  */
trait RelationshipSpecControllerInterface extends BaseSpecControllerInterface

@sfxml
class RelationshipSpecController(@FXML descriptionField: JFXTextArea,
                                 @FXML sourceRefField: JFXTextField,
                                 @FXML targetRefField: JFXTextField,
                                 @FXML relationshipTypeField: JFXTextField
                                ) extends RelationshipSpecControllerInterface {

  var currentForm: RelationshipForm = _


  private def loadValues(): Unit = {
    descriptionField.setText(currentForm.description.value)
    sourceRefField.setText(currentForm.source_ref.value)
    targetRefField.setText(currentForm.target_ref.value)
    relationshipTypeField.setText(currentForm.relationship_type.value)
  }

  def clear(): Unit = {
    unbindAll()
    descriptionField.setText("")
    sourceRefField.setText("")
    targetRefField.setText("")
    relationshipTypeField.setText("")
  }

  private def unbindAll(): Unit = {
    if (currentForm != null) {
      currentForm.source_ref.unbind()
      currentForm.target_ref.unbind()
      currentForm.relationship_type.unbind()
      currentForm.description.unbind()
      currentForm = null
    }
  }

  def control(stix: CyberObj, controller: Option[BundleViewControllerInterface]): Unit = {
    unbindAll()
    if (stix != null) {
      currentForm = stix.asInstanceOf[RelationshipForm]
      loadValues()
      // bind the form to the UI
      currentForm.description <== descriptionField.textProperty()
      currentForm.source_ref <== sourceRefField.textProperty()
      currentForm.target_ref <== targetRefField.textProperty()
      currentForm.relationship_type <== relationshipTypeField.textProperty()
    }
  }

}