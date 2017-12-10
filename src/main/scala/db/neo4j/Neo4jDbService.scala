package db.neo4j

import java.io.File

import com.kodekutters.stix.Bundle
import com.typesafe.config.{Config, ConfigFactory}
import controllers.CyberStationControllerInterface
import cyber.CyberBundle
import db.DbService

import scala.concurrent.ExecutionContext.Implicits.global
import org.neo4j.graphdb.{GraphDatabaseService, Node}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.index.Index
import reactivemongo.api.commands.MultiBulkWriteResult

import scala.concurrent.Future
import scala.util.Try
import scalafx.scene.paint.Color


/**
  * the GraphDatabaseService support and associated index
  */
object Neo4jDbService extends DbService {

  val config: Config = ConfigFactory.load

  var graphDB: GraphDatabaseService = _

  var idIndex: Index[Node] = _

  /**
    * initialise this singleton
    *
    * @param dbDir dbDir the directory of the database
    */
  def init(dbDir: String): Unit = {
    // will create a new database or open the existing one
    Try(graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbDir))).toOption match {
      case None =>
        println("cannot access " + dbDir + ", ensure no other process is using this database, and that the directory is writable")
        System.exit(1)
      case Some(gph) =>
        registerShutdownHook
        println("connected")
        transaction {
          idIndex = graphDB.index.forNodes("id")
        }.getOrElse(println("---> could not process indexing in DbService.init()"))
    }
  }

  // general transaction support
  // see snippet: http://sandrasi-sw.blogspot.jp/2012/02/neo4j-transactions-in-scala.html
  private def plainTransaction[A <: Any](db: GraphDatabaseService)(dbOp: => A): A = {
    val tx = synchronized {
      db.beginTx
    }
    try {
      val result = dbOp
      tx.success()
      result
    } finally {
      tx.close()
    }
  }

  /**
    * do a transaction that evaluate correctly to Some(result) or to a failure as None
    *
    * returns an Option
    */
  def transaction[A <: Any](dbOp: => A): Option[A] = Try(plainTransaction(Neo4jDbService.graphDB)(dbOp)).toOption

  def closeAll() = {
    graphDB.shutdown()
  }

  private def registerShutdownHook =
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run = graphDB.shutdown()
    })

  val dbUri = ""

  def isConnected() = true

  def init(): Unit = {

  }

  def close(): Unit = {

  }

  def saveServerBundle(bundle: Bundle, colPath: String): Unit = {

  }

  def saveLocalBundles(cyberList: List[CyberBundle]): Future[(MultiBulkWriteResult, MultiBulkWriteResult)] = {
    ???
  }

  def loadLocalBundles(): Future[List[CyberBundle]] = {
    ???
  }

  def dropLocalBundles(): Unit = {

  }

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
      val neoLoader = new Neo4jLoader(file, dbDir)
      if (file.getName.toLowerCase.endsWith(".json")) neoLoader.processBundleFile()
      if (file.getName.toLowerCase.endsWith(".zip")) neoLoader.processBundleZipFile()
      controller.showThis("Done saving: " + file.getName + " to Neo4jDB at: " + dbDir, Color.Black)
      controller.showSpinner(false)
    })
  }


}
