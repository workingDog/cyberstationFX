//package support
//
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.{Failure, Success, Try}
//
//
//sealed class TimeoutException extends RuntimeException
//
//object FutureUtils {

//  import scala.concurrent.ExecutionContext.Implicits.global

//  implicit class FutureTimeoutLike[T](f: Future[T]) {
//    def withTimeout(ms: Long): Future[T] = Future.firstCompletedOf(List(f, Future {
//      Thread.sleep(ms)
//      throw new TimeoutException
//    }))
//
//    lazy val withTimeout: Future[T] = withTimeout(2000) // default 2s timeout
//  }

  // does not work
  // code from: https://stackoverflow.com/questions/29344430/scala-waiting-for-sequence-of-futures
//  def lift[T](f: Future[T])(implicit ec: ExecutionContext): Future[Try[T]] =
//    f map {
//      Success(_)
//    } recover { case e => Failure(e) }
//
//  def lift[T](fs: Seq[Future[T]])(implicit ec: ExecutionContext): Seq[Future[Try[T]]] =
//    fs map {
//      lift(_)
//    }
//
//  implicit class RichSeqFuture[+T](val fs: Seq[Future[T]]) extends AnyVal {
//    def onComplete[U](f: Seq[Try[T]] => U)(implicit ec: ExecutionContext) = {
//      Future.sequence(lift(fs)) onComplete {
//        case Success(s) => f(s)
//        case Failure(e) => throw e // will never happen, because of the Try lifting
//      }
//    }
//  }

//}