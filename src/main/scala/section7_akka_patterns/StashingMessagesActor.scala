package section7_akka_patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

/**
 * Stashing
 *
 * * put messages aside for later
 * * prepend them back to the mailbox when te time is right
 */

class StashingMessagesActor
  extends TestKit(ActorSystem("StashingMessagesActor"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import StashingMessagesActor._

  "StashingMessagesActor - resource actor which has a locked access to a resource" should {
    "when resource actor is open it can recieve rread or write request" in {
      val ra = system.actorOf(Props[ResourceActor], "ResourceActor")
      ra ! Read
      ra ! Open
      ra ! Open
      ra ! Write("stash is unsful")
      ra ! Close
      ra ! Read
      Thread.sleep(2000)

      /**
       * memeory bounds on stash
       * depending on mailbox unstash may cause issues
       * mixin the Stash trait LAST
       */

    }
  }
}


object StashingMessagesActor{
  case object Open
  case object Close
  case object Read
  case class Write(data:String)

  /**
   * ResourceActor
   *  - open -> it can recieve read/write requests to the resource
   *  - otherwise it will postpone all read/write requests until the state is open
   *
   *  ResourceActor is closed
   *    - Open => switch to the open state
   *    - Read, Write messages are POSTPONED
   *
   *  ResourceActor is open
   *    - Read, Write are handled
   *    - Close => switch to closed state
   *
   *  e.g
   *  [Open, Read, Read, Write]
   *  - switch to open state
   *  - read data
   *  - read data
   *  - write data
   *
   *  [Read, Open, Write]
   *  - stash Read
   *  - switch to open state
   *    =. Reda is prepended to the mailbox, mailback is now [READ, WRITE]
   *  - read data
   *  - write data
   */

  //1. mixin in the stash trait
  class ResourceActor extends Actor with ActorLogging with Stash{
    private var innerData:String = ""

    def closed:Receive = {
      case Open => {
        //3. unstash messages we can now handle
        unstashAll
        context.become(open)
      }
      case _ => {
        log.info("stashing message")
        //2. stash messages we cant handle yet
        stash
      }
    }

    def open: Receive = {
      case Close => {
        log.info("Closing resource")
        unstashAll
        context.become(closed)
      }
      case Read => {
        log.info(s"reading ...$innerData")
      }
      case Write(message) => innerData += message
      case message => stash
    }

    override def receive: Receive = closed
  }
}