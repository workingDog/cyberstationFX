package db.neo4j

import java.io.File

import com.kodekutters.neo4j.{Neo4jFileLoader, Neo4jLoader}
import com.kodekutters.stix.StixObj
import com.typesafe.config.{Config, ConfigFactory}
import controllers.CyberStationControllerInterface

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafx.scene.paint.Color


/**
  * the Neo4j graph database services
  * delegate all to stixtoneo4jlib
  */
object Neo4jService {

  val config: Config = ConfigFactory.load

  def saveFileToDB(file: File, controller: CyberStationControllerInterface): Unit = {
    controller.showSpinner(true)
    Future({
      var dbDirectory = new java.io.File(".").getCanonicalPath + "/cyberstix"
      try {
        dbDirectory = config.getString("neo4jdb.directory")
      } catch {
        case e: Throwable => println("---> config error: " + e)
      }
      val dbDir = if (dbDirectory.isEmpty) new java.io.File(".").getCanonicalPath + "/cyberstix" else dbDirectory
      println("---> neo4jDB directory: " + dbDir)
      controller.showThis("---> saving: " + file.getName + " to Neo4jDB at: " + dbDir, Color.Black)
      val neoLoader = new Neo4jFileLoader(dbDir)
      if (file.getName.toLowerCase.endsWith(".json")) neoLoader.loadBundleFile(file)
      if (file.getName.toLowerCase.endsWith(".zip")) neoLoader.loadBundleZipFile(file)
      controller.showThis("Done saving: " + file.getName + " to Neo4jDB at: " + dbDir, Color.Black)
      controller.showSpinner(false)
    })
  }

  def saveStixToNeo4j(stixList: List[StixObj], controller: CyberStationControllerInterface): Unit = {
    controller.showSpinner(true)
    Future({
      var dbDirectory = new java.io.File(".").getCanonicalPath + "/cyberstix"
      try {
        dbDirectory = config.getString("neo4jdb.directory")
      } catch {
        case e: Throwable => println("---> config error: " + e)
      }
      val dbDir = if (dbDirectory.isEmpty) new java.io.File(".").getCanonicalPath + "/cyberstix" else dbDirectory
      controller.showThis("---> saving STIX to Neo4jDB at: " + dbDir, Color.Black)
      val neoLoader = new Neo4jLoader(dbDir)
      stixList.foreach(stix => neoLoader.loadIntoNeo4j(stix))
      neoLoader.close()
      controller.showThis("Done saving STIX to Neo4jDB at: " + dbDir, Color.Black)
      controller.showSpinner(false)
    })
  }

}
