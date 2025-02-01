package section4_testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration.DurationInt
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike //BDD style testing
  with BeforeAndAfterAll
  {

  /**
   * Naming convention for test class names is to end with "Spec"
   */

    //setup
    override def afterAll:Unit = {
      TestKit.shutdownActorSystem(system)
    }

    import BasicSpec._
    //basic structure of a test suite
    "A simple actor " should {
      //testing scenario
      "echo back the same message" in {
        val echoActor = system.actorOf(Props[SimpleActor], "simpleActor")
        val msg =  "hello, test"
        echoActor ! msg
        expectMsg("hello, test")
      }
    }

    "A backhole actor " should {
      //testing scenario
      "echo back the same message" in {
        val blackholeActor = system.actorOf(Props[BlackholeActor], "backholeActor")
        val msg =  "hello, test"
        blackholeActor ! msg
        expectNoMessage(1 second)
//        expectMsg("hello, test") //fails with timeout exception, timeout defaults to 3 seconds,
        // this can be changed with property 'akka.test.single-expect-default', this will always fail as the actor does nothing with the message
      }
    }

    "A labtestactor" should {
      val labTestActor = system.actorOf(Props[LabTestActor], "LabTestActor")
      "turn string to uppercase with expect" in {
        labTestActor ! "lowercase!"
        expectMsg("LOWERCASE!")
      }
      "turn string to uppercase with reply" in {
        labTestActor ! "lowercase!"
        val reply = expectMsgType[String]
        assert(reply == "LOWERCASE!")
      }
      "reply to a greeting" in {
        labTestActor ! "greeting"
        expectMsgAnyOf("Hi", "Hello")
      }
      "reply with favorite tech" in {
        labTestActor ! "favoriteTech"
        expectMsgAllOf("Scala", "Akka")
      }

      "reply with cool tech" in {
        labTestActor ! "favoriteTech"
        val message = receiveN(2) // Seq[Any]
        assert(message.size == 2)
      }
      "reply with cool tech in a fancy way" in {
        labTestActor ! "favoriteTech"
        expectMsgPF(){
          case "Scala" => assert(true)
          case "Akka" => assert(true)
        }
      }

    }
}

object BasicSpec{
  class SimpleActor extends Actor{
    override def receive: Receive = {
      case message => sender ! message
    }
  }

  class BlackholeActor extends Actor{
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor{
    val random = new Random()
    override def receive: Receive = {
      case "greeting" =>  sender ! (if(random.nextBoolean) "Hi" else "Hello")
      case "favoriteTech" => {
        sender ! "Scala"
        sender ! "Akka"
      }
      case message:String => sender ! message.toUpperCase
    }
  }
}
