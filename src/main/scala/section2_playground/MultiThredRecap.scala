package section2_playground

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MultiThredRecap extends App{

  //creating threads on the jvm
  val aTHread = new Thread(() => println("im running async"))
  aTHread.start
  aTHread.join

  //different runs produce different results, OS threads
  val t1 =  new Thread(() => (1 to 1000).foreach(i => println("hello")))
  val t2 =  new Thread(() => (1 to 1000).foreach(i => println("goodbye")))
  t1.start
  t2.start
  t1.join
  t2.join

  class BankAccount(amt:Double){
    override def toString: String = super.toString

    //not good practive "sync block
    def withDraw(withAmt:Double)  = this.synchronized{
      this.amt - withAmt
    }
  }

  //inter process communication
  // wait/notify functions

  //scala futures, JVM logical threads
  import scala.concurrent.ExecutionContext.Implicits.global
  val f = Future {42}
  //callbacks
  f.onComplete{
    case Success(42) => println("successs 42")
    case Failure(exception) => println(exception)
  }

  val f2 = f.map(_+1)
  val f3 = f2.flatMap(value => Future{value+2}) //future with 44

  val filterf4 =f3.filter(_ %2 == 0) //NoSuchExement exception

  val f5 = for {
    o <- f
    oo <- filterf4
  } yield (o - oo)

  f5
}
