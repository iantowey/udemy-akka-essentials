package section6_akka_infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

/**
 *  - mailboxes control how messages are stored for actors
 *  - implemnt a custom mailbox and configure it, data structures that store messages in akka
 */

class MailBoxesOutline
  extends TestKit(ActorSystem("MailBoxesOutline", ConfigFactory.load().getConfig("mailboxes-demo")))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import MailBoxesOutline._

  "actor mailbox demos " should {
    "1 custom priority mailbox" in {
      val sa = system.actorOf(Props[SimpleActor])
      //add some priority to messages, lower numbers mean higher priority
      // P0 -> most important
      // P1
      // P2
      // P3 -> least important

      //step 4 - attached a dispatcher to a given actor
      val supportTicketActor = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
      supportTicketActor ! PoisonPill
      supportTicketActor ! "[P3] something not important"
      supportTicketActor ! "[P2] something even less not important "
      supportTicketActor ! "[P0] get on it"
      supportTicketActor ! "[P1] urgent"
      supportTicketActor ! "hello!"


      //the priority mailbox with sort the above message using the partial function
      Thread.sleep(5000)
    }
    "2 control aware mailbox" in {
      /**
       * some tickets need to be processed first, regurdless of the queue - UnboundedControlAwareMailbox
       */
      //step 1 - mark a message as priority via a ControlMessage
      //step 2 - configure who gets to access this mailbox, atach a actor to a mailbox
      val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
      controlAwareActor ! "[P3] something not important"
      controlAwareActor ! "[P2] something even less not important "
      controlAwareActor ! "[P0] get on it"
      controlAwareActor ! "[P1] urgent"
      controlAwareActor ! "hello!"
      controlAwareActor ! ManagementTicket
    }
    "3. using the deployment config" in {
      val alternativeControlAwareActor  = system.actorOf(Props[SimpleActor], "alternativeControlAwareActor")
      alternativeControlAwareActor ! "[P3] something not important"
      alternativeControlAwareActor ! "[P2] something even less not important "
      alternativeControlAwareActor ! "[P0] get on it"
      alternativeControlAwareActor ! "[P1] urgent"
      alternativeControlAwareActor ! "hello!"
      alternativeControlAwareActor ! ManagementTicket
    }
  }
}


object MailBoxesOutline{
  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  // step1 - mailbox def
  class SupportTicketPriorityMailbox(settings:ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(PriorityGenerator {
      case message:String if message.startsWith("[P0]") => 0
      case message:String if message.startsWith("[P1]") => 1
      case message:String if message.startsWith("[P2]") => 2
      case message:String if message.startsWith("[P3]") => 3
      case message:String if message.startsWith("[P4]") => 4
      case _ => 99
    }){
  }
  // step 2 - make it known in the configuration "application.conf"

  //  **************************
  case object ManagementTicket extends ControlMessage

}