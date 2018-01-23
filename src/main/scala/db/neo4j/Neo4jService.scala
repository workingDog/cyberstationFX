package db.neo4j

import java.io.File

import com.kodekutters.neo4j.{Neo4jFileLoader, Neo4jLoader}
import com.kodekutters.stix.{Bundle, StixObj}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import controllers.CyberStationControllerInterface
import org.neo4j.graphdb.{Direction, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafx.scene.paint.Color
import cyber.CyberBundle

/**
  * the Neo4j graph database services
  * delegate all to stixtoneo4jlib
  */
object Neo4jService {

  val config: Config = ConfigFactory.load

  implicit val logger = Logger("CyberStation")

  // create a temp directory name for the local neo4j db
  var theTempPath = new java.io.File(".").getCanonicalPath + "/tempNeo"

  def saveFileToDB(file: File, controller: CyberStationControllerInterface): Unit = {
    controller.showSpinner(true)
    var dbDirectory = new java.io.File(".").getCanonicalPath + "/cyberstix"
    try {
      dbDirectory = config.getString("neo4jdb.directory")
    } catch {
      case e: Throwable => println("---> config error: " + e)
    }
    val dbDir = if (dbDirectory.isEmpty) new java.io.File(".").getCanonicalPath + "/cyberstix" else dbDirectory
    println("---> neo4jDB directory: " + dbDir)
    controller.showThis("Saving: " + file.getName + " to Neo4jDB at: " + dbDir, Color.Black)
    Future({
      val neoLoader = new Neo4jFileLoader(dbDir)
      if (file.getName.toLowerCase.endsWith(".json")) neoLoader.loadBundleFile(file)
      if (file.getName.toLowerCase.endsWith(".zip")) neoLoader.loadBundleZipFile(file)
      controller.showThis("Done saving: " + file.getName + " to Neo4jDB at: " + dbDir, Color.Black)
      controller.showSpinner(false)
    })
  }

  def saveStixToNeo4j(stixList: List[StixObj], controller: CyberStationControllerInterface): Unit = {
    controller.showSpinner(true)
    var dbDirectory = new java.io.File(".").getCanonicalPath + "/cyberstix"
    try {
      dbDirectory = config.getString("neo4jdb.directory")
    } catch {
      case e: Throwable => println("---> config error: " + e)
    }
    val dbDir = if (dbDirectory.isEmpty) new java.io.File(".").getCanonicalPath + "/cyberstix" else dbDirectory
    controller.showThis("---> saving STIX to Neo4jDB at: " + dbDir, Color.Black)
    Future({
      val neoLoader = new Neo4jLoader(dbDir)
      stixList.foreach(stix => neoLoader.loadIntoNeo4j(stix))
      neoLoader.close()
      controller.showThis("Done saving STIX to Neo4jDB at: " + dbDir, Color.Black)
      controller.showSpinner(false)
    })
  }

  def saveStixWith(neoLoader: Neo4jLoader, stixList: List[StixObj], controller: CyberStationControllerInterface): Unit = {
    controller.showSpinner(true)
    if (stixList.nonEmpty) {
      Future({
        controller.showThis("Saving STIX to Neo4jDB at: " + neoLoader.dbDir, Color.Black)
        stixList.foreach(stix => neoLoader.loadIntoNeo4j(stix))
        controller.showThis("Done saving STIX to Neo4jDB at: " + neoLoader.dbDir, Color.Black)
        controller.showSpinner(false)
      })
    }
  }

  def clearDbWith2(neoLoader: Neo4jLoader): Future[Option[Result]] = {
    Future({
      val clearX = "MATCH (n) DETACH DELETE n"
      val clearAction = "MATCH (n) OPTIONAL MATCH (n)-[r]-() WITH n,r LIMIT 50000 DELETE n,r RETURN count(n) as deletedNodesCount"
      neoLoader.neoService.transaction {
        neoLoader.neoService.graphDB.execute(clearAction)
      }
    })
  }

  def clearDbWith(neoLoader: Neo4jLoader) =
    neoLoader.neoService.transaction {
      neoLoader.neoService.graphDB.getAllRelationships.forEach(_.delete())
      neoLoader.neoService.graphDB.getAllNodes.forEach(_.delete())
    }

  def reload(cyberBundles: List[CyberBundle], controller: CyberStationControllerInterface) = {
    Future({
      println("---> reload: " + theTempPath)
      val neoLoader = new Neo4jLoader(theTempPath)
      clearDbWith(neoLoader)
      val theCyberStixs = for (bndl <- cyberBundles) yield bndl.objects.toList
      val theStixs = for (cybr <- theCyberStixs.flatten) yield cybr.toStix
      theStixs.foreach(s => println("----> saving: " + s.`type`))
      controller.showThis("Saving STIX to Neo4jDB at: " + neoLoader.dbDir, Color.Black)
      theStixs.foreach(stix => neoLoader.loadIntoNeo4j(stix))
      controller.showThis("Done saving STIX to Neo4jDB at: " + neoLoader.dbDir, Color.Black)
      controller.showSpinner(false)
      neoLoader.close()
      println("----> done reload")
    })
  }

}
