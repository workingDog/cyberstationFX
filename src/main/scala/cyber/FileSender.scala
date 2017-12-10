package cyber

import java.io.File

import scala.concurrent.ExecutionContext.Implicits.global
import com.kodekutters.stix.Bundle
import com.kodekutters.taxii.Collection
import controllers.CyberStationControllerInterface
import play.api.libs.json.Json
import scala.io.Source
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection.JavaConverters._
import scalafx.scene.paint.Color

/**
  * helper to send a file to a TAXII-2 server
  */
object FileSender {

  def sendBundle(theFile: File, cyberController: CyberStationControllerInterface): Unit = {
    cyberController.showSpinner(true)
    // try to load the data from file
    try {
      cyberController.showThis("Reading bundle file: " + theFile.getName, Color.Black)
      // read a bundle from theFile
      val jsondoc = Source.fromFile(theFile).mkString
      // create a bundle object from it
      Json.fromJson[Bundle](Json.parse(jsondoc)).asOpt match {
        case Some(bundle) => sendBundleToServer(bundle, cyberController)
        case None =>
          cyberController.showThis("Invalid JSON --> fail to load bundle from file: " + theFile.getName, Color.Red)
          println("---> bundle loading failure --> invalid JSON")
      }
    } catch {
      case ex: Throwable =>
        cyberController.showThis("Fail to send the bundle to the server: " + theFile.getName, Color.Red)
    } finally {
      cyberController.showSpinner(false)
    }
  }

  private def sendBundleToServer(theBundle: Bundle, cyberController: CyberStationControllerInterface) = {
    val taxiiApiroot = Option(cyberController.getSelectedApiroot().value)
    val taxiiCol = Option(cyberController.getSelectedCollection().value)
    if (taxiiCol.isEmpty || taxiiApiroot.isEmpty) {
      cyberController.showThis("Cannot send --> no server and collection selected ", Color.Red)
    } else {
      cyberController.showThis("Sending bundle to server ....", Color.Black)
      taxiiCol.map(colInfo => {
        taxiiApiroot.map(apiroot => {
          val col = Collection(colInfo, apiroot)
          if (colInfo.can_write) {
            col.addObjects(theBundle).map(status => {
              println("----> status: " + status.getOrElse(theBundle.id + " could not be sent to the server"))
              cyberController.showThis("Status: " + status.getOrElse(theBundle.id + " could not be sent to the server"), Color.Red)
              col.conn.close()
            })
            // show message on messageBar
            cyberController.showThis(theBundle.id + " sent to the server", Color.Black)
          } else {
            println("---> not sent, " + colInfo.title + " cannot be written to")
            cyberController.showThis("Not sent --> " + colInfo.title + " cannot be written to", Color.Red)
          }
        })
      })
    }
  }

  def sendZipBundles(theFile: File, cyberController: CyberStationControllerInterface): Unit = {
    cyberController.showSpinner(true)
    // try to load the data from a zip file
    try {
      val rootZip = new java.util.zip.ZipFile(theFile)
      rootZip.entries.asScala.foreach(stixFile => {
        // read a STIX bundle from the InputStream
        val jsondoc = Source.fromInputStream(rootZip.getInputStream(stixFile)).mkString
        // create a bundle object
        Json.fromJson[Bundle](Json.parse(jsondoc)).asOpt match {
          case Some(bundle) => sendBundleToServer(bundle, cyberController)
            println("---> trying to send " + stixFile.getName + " to server")
          case None =>
            cyberController.showThis("Fail to load bundle from file: " + stixFile.getName, Color.Red)
            println("---> bundle loading failure, invalid json in: " + stixFile.getName)
        }
      })
    } catch {
      case ex: Throwable =>
        println("---> Fail to load bundles from file: " + theFile.getName)
        cyberController.showThis("Fail to load bundles from file: " + theFile.getName, Color.Red)
    }
    finally {
      cyberController.showSpinner(false)
    }
  }

}
