package db

import com.kodekutters.stix.{Bundle, Timestamp}
import play.api.libs.json.Json
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}
import reactivemongo.api._
import reactivemongo.play.json.collection.JSONCollection
import util.Utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import reactivemongo.play.json._

import scala.concurrent.duration._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import cyber.{BundleInfo, CyberBundle}

import scala.collection.mutable
import scala.util.{Failure, Success}


/*

trait AccountDao {
  def save(user: Account): Future[WriteResult]

  def find(accId: UUID): Future[Option[Account]]

  def update(acc: Account): Future[WriteResult]

  def clearAccounts(): Future[WriteResult]
}


      // the db location path
      val dbf = if (args.length == 3) args(2).trim else ""
      // if nothing default is current location path plus stixdb
    //  val dbFile = if (dbf.isEmpty) new java.io.File(".").getCanonicalPath + "/stixdb" else dbf
      val dbFile = new java.io.File(".").getCanonicalPath + "/stixdb"

        // initialise the mongodb
  MongoDbService.init(mongodb)
 //  def msgF = MongoDbService.database.map(_.collection[JSONCollection](collectionName))

//  def msgF = database.map(_.collection[JSONCollection](msgCol))



  val withDatabase: Boolean = configuration.getBoolean("mikan.withdatabase", false)

  private val logger = org.slf4j.LoggerFactory.getLogger("db.AccountDao")

  private val accountCol: String = playConf.getString("mongo.collection.accounts").getOrElse("accounts")

  private def accountsF: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection](accountCol))

  def find(accId: UUID): Future[Option[Account]] = for {
    accounts <- accountsF
    user <- accounts.find(Json.obj("accId" -> accId)).one[Account]
  } yield user

  def save(acc: Account): Future[WriteResult] = for {
      accounts <- accountsF
      lastError <- accounts.insert(acc)
    } yield lastError

  def update(acc: Account): Future[WriteResult] = {
    val doc = Json.toJson(acc).as[JsObject]
    accountsF.flatMap(_.update(Json.obj("accId" -> acc.accId), doc))
  }

  def clearAccounts(): Future[WriteResult] =  for {
      logs <- accountsF
      result <- logs.remove(Json.obj())
    } yield result



 */


/**
  * the MongoDbService support
  */
object MongoDbService {

  val config: Config = ConfigFactory.load

  var database: Future[DefaultDB] = _

  val bundlesCol: String = config.getString("mongo.collection.bundles")
  val bundlesInf: String = config.getString("mongo.collection.bundlesInfo")

  def bundlesF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesCol))

  def bundlesInfoF: Future[JSONCollection] = database.map(_.collection[JSONCollection](bundlesInf))


  /**
    * initialise this singleton
    */
  def init(): Unit = {
    // will create a new database or open the existing one
    //  val driver1 = new reactivemongo.api.MongoDriver
    //  val connection3 = driver1.connection(List("localhost"))

    //  val conOpts = MongoConnectionOptions(/* connection options */)
    //  val connection4 = driver1.connection(List("localhost"), options = conOpts)

    val mongoUri = config.getString("mongodb.uri")
    println("trying to connect to: " + mongoUri)

    val driver = new MongoDriver

    database = for {
      uri <- Future.fromTry(MongoConnection.parseURI(mongoUri))
      con = driver.connection(uri)
      dn <- Future(uri.db.get)
      db <- con.database(dn)
    } yield db

    database.onComplete {
      case Success(theDB) => println(s"mongodb connected to: $theDB")
      case Failure(err) => println(s"mongodb fail to connect, error: $err")
    }
  }

  def close(): Unit = database.map(db => db.connection.close())

  /**
    * create all collections from the STIX objects type names (including Bundle)
    */
  def createStixCollections(): Unit = {
    database.map(db => Utils.listOfObjectTypes.foreach(objType => db.collection[JSONCollection](objType)))
  }

  def saveBundle(bundle: Bundle): Future[WriteResult] = for {
    bundles <- bundlesF
    lastError <- bundles.insert(bundle)
  } yield lastError

  def saveAllBundles(cyberList: List[CyberBundle]): Future[(MultiBulkWriteResult, MultiBulkWriteResult)] = {
    // the list of STIX bundles
    val bundleList = for (item <- cyberList) yield item.toStix
    // create the bundles info list
    val infoList = for (item <- cyberList) yield new BundleInfo("userx",
      item.id.value, item.name.value, Timestamp.now().toString())
    // insert all bundles and info
    for {
      bundles <- bundlesInfoF
      infErrors <- bundles.insert[BundleInfo](ordered = true).many(infoList)
      bundles <- bundlesF
      errors <- bundles.insert[Bundle](ordered = true).many(bundleList)
    } yield (infErrors, errors)
  }

  def loadBundlesInfo(): Future[List[BundleInfo]] = for {
    bundleInfo <- bundlesInfoF
    theList <- bundleInfo.find(Json.obj()).
      cursor[BundleInfo](ReadPreference.nearest).
      collect[List](-1, Cursor.FailOnError[List[BundleInfo]]())
  } yield theList

  def loadBundles(): Future[List[Bundle]] = for {
    bundles <- bundlesF
    bundleList <- bundles.find(Json.obj()).
      cursor[Bundle](ReadPreference.nearest).
      collect[List](-1, Cursor.FailOnError[List[Bundle]]())
  } yield bundleList

  def loadCyberBundles(): Future[List[CyberBundle]] = for {
    bundles <- loadBundles()
    infoList <- loadBundlesInfo()
    cyberBundles <- Future(for (bndl <- bundles) yield {
      val info = infoList.find(x => x.bundle_id == bndl.id.toString()).getOrElse(BundleInfo.emptyInfo())
      CyberBundle.fromStix(bndl, info.name)})
  } yield cyberBundles

  def dropBundles(): Unit = bundlesF.map(bndls => bndls.drop(true))

  def dropBundlesInfo(): Unit = bundlesInfoF.map(bndls => bndls.drop(true))

  def dropAllBundles(): Unit = {
    dropBundles()
    dropBundlesInfo()
  }

}

