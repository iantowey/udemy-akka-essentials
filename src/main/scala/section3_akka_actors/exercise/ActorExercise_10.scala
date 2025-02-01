package section3_akka_actors.exercise

import akka.actor.{Actor, ActorSystem, Props}
import section3_akka_actors.GoNuts

object ActorExercise_10 extends App {

  /**
   * Exercises
   *
   * 1. create a counter actor, it will hold an internal variable and will handle messages
   * -  INCREMENT
   * -  DECREMENT
   * -  PRINT
   *
   * 2. create Bank account actor, it will hold an internal variable it will handle messages
   * -  WITHDRAW => WILL REPLY WITH success/failure
   * -  DEPOSIT => WILL REPLY WITH success/failure
   * -  STATEMENT => print to screen
   *
   * Design both the messages and the logic.
   *
   * to verifiy functionality, interact with another actor
   */

  val actorSystem = ActorSystem("actorCapabilities")

  //DOMAIN of the counter (message that the actor supports, put in companion object)
  object CounterActor {
    case object INCREMENT

    case object DECREMENT

    case object PRINT
  }

  class CounterActor extends Actor {

    import CounterActor._

    var cnter = 0

    override def receive: Receive = {
      case INCREMENT => cnter += 1
      case DECREMENT => cnter -= 1
      case PRINT => println(s"[${self.path}] $cnter")
    }
  }

  val counterActor = actorSystem.actorOf(Props[CounterActor], "counterActor")

  (1 to 10).foreach { _ => counterActor ! CounterActor.INCREMENT }
  counterActor ! CounterActor.PRINT
  (1 to 5).foreach { _ => counterActor ! CounterActor.DECREMENT }
  counterActor ! CounterActor.PRINT

  trait Account

  object BankAccountActor {
    case class Withdraw(amt: Double)

    case class Deposit(amt: Double)

    case object STATEMENT

    case class Success(msg: String)

    case class Failure(msg: String)
  }

  import BankAccountActor._

  class BankAccountActor extends Actor {

    import BankAccountActor._

    var balance = 0.0

    override def receive: Receive = {
      case Withdraw(amt) => {
        if (balance >= amt) {
          if (amt < 0) {
            context.sender ! Failure(s"Invalid amount $amt")
          } else {
            balance -= amt
            context.sender ! Success(s"$amt withdrawn")
          }
        } else {
          context.sender ! Failure(s"insufficient funds")
        }
      }
      case Deposit(amt) => {
        balance += amt
        context.sender ! Success(s"$amt deposited")
      }
      case STATEMENT => println(s"current balance: $balance")
    }
  }

  val bankAccountActor = actorSystem.actorOf(Props[BankAccountActor], "bankAccountActor")

  class BankCustomerActor extends Actor {
    override def receive: Receive = {
      case GoNuts(bankAccActor) => {
        bankAccActor ! Withdraw(100.0)
        bankAccActor ! Deposit(100.0)
        bankAccActor ! Deposit(100.0)
        bankAccActor ! Deposit(100.0)
        bankAccActor ! Withdraw(100.0)
        bankAccActor ! STATEMENT
      }
      case BankAccountActor.Success(msg) => println(msg)
      case BankAccountActor.Failure(msg) => println(msg)
    }
  }

  val bankCustomerActor = actorSystem.actorOf(Props[BankCustomerActor], "bankCustomerActor")

  bankCustomerActor ! GoNuts(bankAccountActor)

}
