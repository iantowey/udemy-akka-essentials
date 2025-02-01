package section5_faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionOfActorsSpec extends TestKit(ActorSystem("SupervisionOfActorsSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import SupervisionOfActorsSpec._

  "A supervior" should {
    "resume its child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[FussyWordCounterSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "I love akka"
      child ! Report
      expectMsg(3)
      child ! ("I love akka" * 10)
      child ! Report
      expectMsg(3)
    }
    "restart its child in case of an empty string" in {
      val supervisor = system.actorOf(Props[FussyWordCounterSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! ""
      child ! Report
      expectMsg(0)
    }
    "The supervisor should terminate this child in case of a major error" in {
      val supervisor = system.actorOf(Props[FussyWordCounterSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      watch(child)
      child ! "i love akka"
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
    "The supervisor should esclate an error when it does not know what to do" in {
      val supervisor = system.actorOf(Props[FussyWordCounterSupervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! -1
      watch(child)
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }
  "a kinder superviosr " should {
    "not kill sub actors in case its restarted or esclates failures" in {
      val supervisor = system.actorOf(Props[FussyWordCounterSupervisorNoDeath], "FussyWordCounterSupervisorNoDeath")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 43
      child ! Report
      expectMsg(0)
    }
  }
  "an all-for-one supervisor" should {
    "apply  the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForSupervisor], "AllForSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child1 = expectMsgType[ActorRef]
      supervisor ! Props[FussyWordCounter]
      val child2 = expectMsgType[ActorRef]

      child2 ! "I love akka"
      child2 ! Report
      expectMsg(3)

      EventFilter[NullPointerException]() intercept {
        child1 ! "" //asserts this child throw NPE
      }
      Thread.sleep(500) //hack to wait for the child2 to restart
      child2 ! Report
      expectMsg(0) // this child did not fail but the supervisorStrategyof allforone, causes a restart for all childern
    }
  }
}

object SupervisionOfActorsSpec {

  class AllForSupervisor extends FussyWordCounterSupervisor{
    //define a different supervisor strategy
    override val supervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  class FussyWordCounterSupervisorNoDeath extends FussyWordCounterSupervisor {

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {

    }
  }

    class FussyWordCounterSupervisor extends Actor {
    //define a different supervisor strategy
    override val supervisorStrategy:SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props => {
        val childRef = context.actorOf(props)
        sender ! childRef
      }
    }
  }

  case object Report
  class FussyWordCounter extends Actor{
    var words = 0
    override def receive: Receive = {
      case Report => sender ! words
      case "" => throw new NullPointerException("sentence is empty")
      case sentence:String => {
        if (sentence.length > 20) throw new RuntimeException("sentence is too big")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("must start with uppercase")
        else words += sentence.split(" ").length
      }
      case _ => throw new Exception("con only process strings")
      }
  }
}
