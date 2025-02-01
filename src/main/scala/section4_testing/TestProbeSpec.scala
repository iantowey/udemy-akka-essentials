package section4_testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with WordSpecLike //BDD style testing
  with BeforeAndAfterAll {

  //setup
  override def afterAll:Unit = {
    TestKit.shutdownActorSystem(system)
  }

  //Only want to test Master Actor, need something to stand in in place of actor slave
  import TestProbeSpec._
  "A master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") //a special actor with assertion capabilitites
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
    }

    "send the work to the slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") //a special actor with assertion capabilitites
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
      master ! Work("I love akka ")
      slave.expectMsg(SlaveWork("I love akka ", testActor)) //test actor is the implicit sender of all messages
      slave.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") //a special actor with assertion capabilitites
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
      val workString = "I love akka "
      master ! Work(workString)
      master ! Work(workString)

      //not slave probs ?
      slave.receiveWhile(){
        //the objects in quotes must match above values, these are implicit assertions 
        case SlaveWork(`workString`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
      }
      expectMsg(Report(3))
      expectMsg(Report(6))

    }
  }


}

object TestProbeSpec{
  /**
   * word counting actor hierarchy master-slave
   *
   * send work to master
   *  - master sends the slace a piece of work
   *  - slace processed the work and replies top master
   *  - master aggregates result
   * master sends the total count to the original requester
   */

  case class Register(slaveRef:ActorRef)
  case class Work(text:String)
  case class SlaveWork(text:String, originalRequester:ActorRef)
  case class WorkCompleted(count:Int, originalRequester:ActorRef)
  case class Report(totalCount:Int)
  case object RegistrationAck

  class Master extends Actor{

    override def receive: Receive = {
      case Register(slaveRef) => {
        sender ! RegistrationAck
        context.become(online(slaveRef, 0))
      }
      case _ =>
    }

    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender)
      case WorkCompleted(count, originalRequester) => {
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(slaveRef, newTotalWordCount))
      }
    }

  }

  //Dont care about this only want to test Master
  //
//  class Slave extends Actor{
//    override def receive: Receive = {
//      case SlaveWork(text, sender) => sender ! WorkCompleted(text.split(" ").length)
//    }
//  }
}