package db.neo4j

import java.io.File

import com.kodekutters.stix.{Bundle, _}
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.{implicitConversions, postfixOps}

/**
  * loads Stix-2.1 objects and relationships into a Neo4j graph database
  *
  * @author R. Wathelet June 2017
  *
  *         ref: https://github.com/workingDog/scalastix
  * @param inFile the input file to process
  * @param dbDir  the db.neo4j graph database directory name
  */
class Neo4jLoader(inFile: String, dbDir: String) {

  import MakerSupport._

  // initialize the db services
  Neo4jDbService.init(dbDir)

  println("---> processing...")

  // the nodes maker for creating nodes and their embedded relations
  val nodesMaker = new NodesMaker()
  // the relations maker for creating relations
  val relsMaker = new RelationsMaker()

  // process a bundle of Stix objects
  def processBundle(bundle: Bundle, fileName: String) = {
    // all nodes and their internal relations are created first
    bundle.objects.foreach(nodesMaker.createNodes(_))
    // the file name being processed
    println("\nfile: " + fileName)
    // print the number of SDO, SRO and StixObj (MarkingDefinition+LanguageContent)
    nodesMaker.counter.foreach({ case (k, v) => println(k + ": " + v) })
    // sum the SDO, SRO and StixObj
    println("total: " + nodesMaker.counter.foldLeft(0)(_ + _._2))
    // all SRO and relations that depends on nodes are created after the nodes
    bundle.objects.foreach(relsMaker.createRelations(_))
  }

  /**
    * read a bundle of Stix objects from the input file,
    * convert it to db.neo4j nodes and relations and load them into the db
    */
  def processBundleFile(): Unit = {
    // read a STIX bundle from the inFile
    val jsondoc = Source.fromFile(inFile).mkString
    Option(Json.parse(jsondoc)) match {
      case None => println("\n-----> could not parse JSON in file: " + inFile)
      case Some(js) =>
        // create a bundle object from it and convert its objects to nodes and relations
        Json.fromJson[Bundle](js).asOpt match {
          case None => println("\n-----> ERROR reading bundle in file: " + inFile)
          case Some(bundle) => processBundle(bundle, inFile)
        }
        Neo4jDbService.closeAll()
    }
  }

  /**
    * read Stix bundles from the input zip file and
    * convert them to db.neo4j nodes and relations and load them into the db
    */
  def processBundleZipFile(): Unit = {
    import scala.collection.JavaConverters._
    // get the zip file
    val rootZip = new java.util.zip.ZipFile(new File(inFile))
    // for each entry file containing a single bundle
    rootZip.entries.asScala.filter(_.getName.toLowerCase.endsWith(".json")).foreach(f => {
      loadBundle(rootZip.getInputStream(f)) match {
        case Some(bundle) => processBundle(bundle, inFile + " --> " + f.getName)
        case None => println("-----> ERROR invalid bundle JSON in zip file: \n")
      }
    })
    Neo4jDbService.closeAll()
  }

  /**
    * For processing very large text files.
    *
    * read Stix objects one by one from the input file,
    * convert them to db.neo4j nodes and relations and load them into the db
    *
    * The input file must contain a Stix object on one line ending with a new line.
    *
    */
  def processStixFile(): Unit = {
    // go thru the file twice, on first pass process the nodes, on second pass relations
    for (pass <- 1 to 2) {
      // read a STIX object from the inFile, one line at a time
      for (line <- Source.fromFile(inFile).getLines) {
        Option(Json.parse(line)) match {
          case None => println("\n-----> could not parse JSON in file: " + inFile + " line: " + line)
          case Some(js) =>
            // create a Stix object from it
            Json.fromJson[StixObj](js).asOpt match {
              case None => println("\n-----> ERROR reading StixObj in file: " + inFile + " line: " + line)
              case Some(stixObj) =>
                if (pass == 1)
                  nodesMaker.createNodes(stixObj)
                else
                  relsMaker.createRelations(stixObj)
            }
        }
      }
    }
    Neo4jDbService.closeAll()
  }

  /**
    * For processing very large zip files.
    *
    * read Stix objects one by one from the input zip file,
    * convert them to db.neo4j nodes and relations and load them into the db
    *
    * There can be one or more file entries in the zip file,
    * each file must have the extension ".json".
    *
    * Each entry file must have a Stix object on one line ending with a new line.
    *
    */
  def processStixZipFile(): Unit = {
    // get the input zip file
    val rootZip = new java.util.zip.ZipFile(new File(inFile))
    // for each entry file
    rootZip.entries.asScala.filter(_.getName.toLowerCase.endsWith(".json")).foreach(f => {
      // go thru the file twice, on first pass process the nodes, on second pass relations
      for (pass <- 1 to 2) {
        // get the lines from the entry file
        val inputLines = Source.fromInputStream(rootZip.getInputStream(f)).getLines
        // read a Stix object from the inputLines, one line at a time
        for (line <- inputLines) {
          Option(Json.parse(line)) match {
            case None => println("\n-----> could not parse JSON in file: " + inFile + " line: " + line)
            case Some(js) =>
              // create a Stix object from it
              Json.fromJson[StixObj](js).asOpt match {
                case None => println("\n-----> ERROR reading StixObj in file: " + inFile + " line: " + line)
                case Some(stixObj) =>
                  if (pass == 1)
                    nodesMaker.createNodes(stixObj)
                  else
                    relsMaker.createRelations(stixObj)
              }
          }
        }
      }
    }
    )
    Neo4jDbService.closeAll()
  }

}
