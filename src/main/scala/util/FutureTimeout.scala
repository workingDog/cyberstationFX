package util

import scala.concurrent.Future

sealed class TimeoutException extends RuntimeException

object FutureTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit class FutureTimeoutLike[T](f: Future[T]) {
    def withTimeout(ms: Long): Future[T] = Future.firstCompletedOf(List(f, Future {
      Thread.sleep(ms)
      throw new TimeoutException
    }))

    lazy val withTimeout: Future[T] = withTimeout(2000) // default 2s timeout
  }

}