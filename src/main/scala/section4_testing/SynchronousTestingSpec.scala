package section4_testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends WordSpecLike with BeforeAndAfterAll{

  implicit val system = ActorSystem("SynchronousTestingSpec")

  //setup
  override def afterAll:Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SynchronousTestingSpec._
  "A counter" should {
    //synchronous unit tests - all about testability and deterministic
    "synchronously Inc counter" in {
      val counter = TestActorRef[Counter](counterActorFactory())
      counter ! Inc // counter has already recieved the message becuase TestActorRef is executed in the main thread
      assert(counter.underlyingActor.count == 1)
    }
    //TestActorRef can invoke the
    "synchronously Inc counter at the call to the recieve function" in {
      val counter = TestActorRef[Counter](counterActorFactory())
      counter.receive(Inc) // no need to send message can invoke receieve directly with any message
      assert(counter.underlyingActor.count == 1)
    }

    "use the calling thread dispatcher, more on dispatchers later" in {
      val counter = system.actorOf(Props(counterActorFactory()).withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()
      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 0) // this requires Duration.Zero, this ensures the probe has alrady recieved the message 0
    }
  }
}


object SynchronousTestingSpec{

  case object Inc
  case object Read
  class Counter extends Actor{
    var count = 0
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender ! count
    }
  }
  val counterActorFactory = () => new Counter

}