package converter

import java.io._
import java.nio.file.{Files, Path, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.kodekutters.stix.Bundle
import play.api.libs.json.Json

import scala.io.Source
import scala.language.implicitConversions
import scala.language.postfixOps

/**
  * represents a converter from Stix to some other format,
  * all converters must extends this trait
  */
trait StixConverter {
  def convert(bundle: Bundle): String

  // the output file extension to use (include the leading ".")
  val outputExt: String
}

/**
  * do the transformation of Stix objects and output the results
  *
  * @param converter the converter to use in the transformation
  */
class Transformer(converter: StixConverter) {

  /**
    * reads an input Stix file and write the converted results to an output file with the chosen extension
    *
    * @param file the input Stix file name
    * @param ext the output file name extension, eg: ".gexf"
    */
  def stixFileConvertion(file: File, ext: String): Unit = {
    if (file.getName.toLowerCase.endsWith(".zip")) {
      stixConvertionZip(file.getCanonicalPath, file.getCanonicalPath.dropRight(4) + ext + ".zip")
    } else {
      stixConvertion(file.getCanonicalPath, file.getCanonicalPath + ext)
    }
  }

  /**
    * convert the input Stix file and write the converted results to the output file
    *
    * @param inFile  the input Stix file name must have extension .json
    * @param outFile the results output file
    */
  def stixConvertion(inFile: String, outFile: String): Unit = {
    // read a STIX bundle from a file
    val jsondoc = Source.fromFile(inFile).mkString
    // create a bundle object from it
    Json.fromJson[Bundle](Json.parse(jsondoc)).asOpt match {
      case None => println("\n-----> ERROR reading bundle in file: " + inFile); None
      case Some(bundle) => writeToFile(outFile, converter.convert(bundle))
    }
  }

  def convertToFile(file: File, bundle: Bundle): Unit = {
    writeToFile(file.getCanonicalPath, converter.convert(bundle))
  }

  /**
    * convert the input Stix zip file and write the results to the output zip file
    *
    * @param inFile  the input Stix zip file name
    * @param outFile the results output zip file
    */
  def stixConvertionZip(inFile: String, outFile: String): Unit = {
    val bundleMap = readStixBundleZip(inFile)
    // convert each bundle file to a converted (string) representation
    val graphmlMap = for ((fileName, Some(bundle)) <- bundleMap) yield fileName -> converter.convert(bundle)
    writeToZipFile(outFile, graphmlMap)
  }

  /**
    * get a map of file names and bundles from the input zip file
    *
    * @param inFile the input zip file name
    * @return a map of (zip_file_name_entry and bundle options)
    */
  def readStixBundleZip(inFile: String): Map[String, Option[Bundle]] = {
    import scala.collection.JavaConverters._
    val rootZip = new java.util.zip.ZipFile(new File(inFile))
    rootZip.entries.asScala.
      filter(_.getName.toLowerCase.endsWith(".json")).
      collect { case stixFile => (stixFile.getName, loadBundle(rootZip.getInputStream(stixFile)))
      } toMap
  }

  /**
    * get a Bundle from the input source
    *
    * @param source the input InputStream
    * @return a Bundle option
    */
  def loadBundle(source: InputStream): Option[Bundle] = {
    // read a STIX bundle from the InputStream
    val jsondoc = Source.fromInputStream(source).mkString
    // create a bundle object from it
    Json.fromJson[Bundle](Json.parse(jsondoc)).asOpt match {
      case None => println("-----> ERROR invalid bundle JSON in zip file: \n"); None
      case Some(bundle) => Option(bundle)
    }
  }

  /**
    * write the list of converted objects (string) to the output file
    *
    * @param outFile    the output file to write the results to, if empty to System.out
    * @param graphmlMap the list of converted objects to write
    */
  def writeToFile(outFile: String, graphmlMap: String): Unit = {
    val writer = if (outFile.isEmpty) new PrintWriter(System.out) else new PrintWriter(new File(outFile))
    try {
      writer.write(graphmlMap)
    } catch {
      case e: IOException => e.printStackTrace()
    }
    finally {
      writer.close()
    }
  }

  /**
    * write the list of converted objects (string) to the output zip file
    *
    * @param outFile the output file to write the results to
    * @param theMap  the list of converted objects to write
    */
  def writeToZipFile(outFile: String, theMap: Map[String, String]): Unit = {
    try {
      // write a file for each converted bundle into the zip file
      if (outFile.nonEmpty)
        writeToZip(Paths.get(outFile), theMap)
      else
        println("error: must have an output file if the input is a zip file")
    } catch {
      case e: IOException => e.printStackTrace()
    }
  }

  /**
    * write theMap of file name -> converted format to the zip file out
    *
    * @param out    the output zip file
    * @param theMap of file name -> converted format
    */
  def writeToZip(out: Path, theMap: Map[String, String]) = {
    val zip = new ZipOutputStream(Files.newOutputStream(out))
    theMap.foreach { mapEntry =>
      val fileName = mapEntry._1.replace(".json", converter.outputExt)
      zip.putNextEntry(new ZipEntry(fileName))
      try {
        zip.write(mapEntry._2.getBytes)
      } catch {
        case e: IOException => e.printStackTrace()
      }
      finally {
        zip.closeEntry()
      }
    }
    zip.close()
  }

}
