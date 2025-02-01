package section3_akka_actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import section3_akka_actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}
import section3_akka_actors.ChildActors.Parent.{CreateChild, TellChild}
import section3_akka_actors.exercise.ActorBehaviourExercise_14.CheckAllVotesCounted

object ChildActors extends App{

  val actorSystem = ActorSystem("firstActorSystem")

  //actors can create other actors, by using its context

  object Parent{
    case class CreateChild(name:String)
    case class TellChild(msg:String)
  }
  class Parent extends Actor{
    import Parent._

    override def receive: Receive = init

    def init:Receive = {
      case CreateChild(name) => {
        println(s"[${self.path}] creating child .... ")
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef), false)
      }
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(msg) => {
        println(s"[${self.path}] tell child .... ")
        childRef forward msg
      }
    }

  }

  class Child extends Actor{
    override def receive: Receive = {
      case message:String => println(s"[${self.path}] i got $message")
    }
  }

  val parent = actorSystem.actorOf(Props[Parent], "parent")

  parent ! TellChild("blah,blah,blah")
  parent ! CreateChild("kid_A")
  parent ! TellChild("blah,blah,blah")
//  parent ! CreateChild("kid_B")
//  parent ! TellChild("blah,blah,blah")

  /**
   * actor hierarchies
   *
   * parent -> child -> grandchild
   *        -> child2
   */

  /**
   * Cuardian Actors (top-level)
   *
   * / = root guardian
   *  /system = system guardian (child of /)
   *  /user = user-level guardian (child of /)
   *
   */

  /**
   * Actor Selection
   */
  val childSelection = actorSystem.actorSelection("/user/parent/kid_A")
  childSelection ! "i found you "

  val childSelection2 = actorSystem.actorSelection("/user/parent/kid_not_exists")
  childSelection2 ! "i found you "

  /**
   * Achtung
   *
   * never pass multiple actor state ot the THIS ref to child actors, never in your Life!!
   *
   * this will break encapsulation and will allow the child to execute ,methods in the parent WITHOUT having to send it a message
   */

  object NaiveBankAccount{
    case class Deposit(amt:Double)
    case class Withdrawal(amt:Double)
    case object InitAccount
  }
  class NaiveBankAccount extends Actor{
    import NaiveBankAccount._
    import CreditCard._
    var bal = 0.0
    override def receive: Receive = {
      case InitAccount => {
        val cc = context.actorOf(Props[CreditCard], "card")
        cc ! AttachToAccount(this)
      }
      case Deposit(amt) => deposit(amt)
      case Withdrawal(amt) => withdraw(amt)
    }

    def deposit(funds:Double) = {
      println(s"[${self.path} despositing $funds to $bal]")
      bal+=funds
    }
    def withdraw(funds:Double) = {
      println(s"[${self.path} withdrawing $funds from $bal]")
      bal-=funds
    }
  }

  object CreditCard{
    case class AttachToAccount(bankAcc:NaiveBankAccount) //!! very bad
    case object CheckStatus
  }
  class CreditCard extends Actor{

    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachTo(account))
    }
    def attachTo(account: NaiveBankAccount): Receive = {
      case CheckStatus => println(s"[$self.path] you message has been processed")
      account.withdraw(1) // this line is a problem, we have directly called a parent method from the child,
        // this should never be done!!!! this is bypassing all the security checks etc, remember actors should ONLY
        // be communicated via a message !!!!! exposing your application to concurrency issues

        // this is called "closing over"

        // NEVER "close over" mutable state
    }
  }

  import NaiveBankAccount._, CreateChild._
  val bankAccountRef = actorSystem.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(1000)
  val ccSelection = actorSystem.actorSelection("/user/account/card")
  ccSelection ! CheckStatus
}
