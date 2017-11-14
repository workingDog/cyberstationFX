package db

import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


import scala.language.{implicitConversions, postfixOps}
import scala.collection.JavaConverters._



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

  var database: Future[DefaultDB] = _

  /**
    * initialise this singleton
    *
    * @param dbDir dbDir the directory of the database
    */
  def init(dbDir: String): Unit = {
    // will create a new database or open the existing one
    //  val driver1 = new reactivemongo.api.MongoDriver
    //  val connection3 = driver1.connection(List("localhost"))

    //  val conOpts = MongoConnectionOptions(/* connection options */)
    //  val connection4 = driver1.connection(List("localhost"), options = conOpts)

    val mongoUri = "mongodb://localhost:27017/stix21"

    val driver = new MongoDriver
    database = for {
      uri <- Future.fromTry(MongoConnection.parseURI(mongoUri))
      con = driver.connection(uri)
      dn <- Future(uri.db.get)
      db <- con.database(dn)
    } yield db

    database.onComplete {
      case resolution =>
        //  println(s"DB resolution: $resolution")
        println("Mongodb connected")
      //  driver.close()
    }

  }

}

