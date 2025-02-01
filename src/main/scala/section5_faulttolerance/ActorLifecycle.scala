package section5_faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifecycle extends App{

  object StartChild

  class LifeCycyleActor extends Actor with ActorLogging {

    override def preStart():Unit = log.info("I am starting")

    override def postStop():Unit = log.info("I am stopping")

    override def receive: Receive = {
      case StartChild => context.actorOf(Props[LifeCycyleActor], "child")
    }
  }

  val system = ActorSystem("ActorLifecycle")
//
//  val parent = system.actorOf(Props[LifeCycyleActor], "parent")
//  parent ! StartChild
//  parent ! PoisonPill // this will stop the parent and ALL its childern, the childer are stopped first then the parent

  /**
   * restart
   */

  object Fail
  object FailChild
  object CheckChildStatus
  object CheckStatus


  class Child extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("supervised child started")

    override def postStop(): Unit = log.info("supervised child stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info(s"supervised actor restarted because of ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit = log.info("supervised child restarted")

    override def receive: Receive = {
      case Fail => {
        log.warning("child will fail now")
        throw new RuntimeException("i failed")
      }
      case CheckStatus => log.info("I'm here!!")
    }
  }

  class Parent extends Actor{
    private val child = context.actorOf(Props[Child], "supervisedChild")
    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChildStatus  => child ! CheckStatus
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild
  //child was restarted after runtime exception
  supervisor ! CheckChildStatus
  // the default supervisor stratedgy is to disguard the message that caused the child to fail, there are other superision strategys
}
