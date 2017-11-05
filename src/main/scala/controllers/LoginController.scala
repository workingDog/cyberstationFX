package controllers

import scalafx.Includes._
import scalafx.scene.control.Button

class LoginController(loginButton: Button) {


//  loginButton.onAction = (event: ActionEvent) =>  {
//
//  }

  loginButton.onAction = handle {
    println("--> do login")
  }


}
