package section4_testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import section4_testing.TestProbeSpec.Master

import scala.concurrent.duration.DurationInt
import scala.util.Random

class TimedAssertionsSpec extends TestKit(
  ActorSystem("TimedAssertionsSpec", ConfigFactory.load("application.conf").getConfig("specialTimedAssertionsConfig"))
)
  with ImplicitSender
  with WordSpecLike //BDD style testing
  with BeforeAndAfterAll {

  //setup
  override def afterAll:Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TimedAssertionsSpec._

  "a worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])
    "reply with the meaning of life in a timely manner" in {
      within(500 millis, 1 second){ // at least 500ms at most 1000ms
        workerActor ! "work"
        expectMsg(WorkResult(42)) //if the message comoes back too fast or too late, assertion will fail
      }
    }

    "reply with valid work at a resonable cadence" in {
      workerActor ! "workSequence"
      val results = receiveWhile[Int](max = 2 seconds, idle = 500 millis, messages = 10){
        case WorkResult(result) => result
      }
      assert(results.sum > 5)
    }

    "reply to a test probe in a timely manner" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(workerActor, "work")
        probe.expectMsg(WorkResult(42))
        // testprobes done t  listen to the timeouts from within blocks,
        // it has its own timeout property 'single-expect-default' in the config file 'application.conf'
        // 0.3s fails 1.3s passes
      }
    }
  }

}

object TimedAssertionsSpec {

  case class WorkResult(result:Int)

  class WorkerActor extends Actor{
    override def receive: Receive = {
      case "work" => {
        Thread.sleep(600)
        sender ! WorkResult(42)
      }
      case "workSequence" => {
        val r = new Random
        for( i <- 0 to 10){
          Thread.sleep(r.nextInt(50))
          sender ! WorkResult(1)
        }
      }
    }
  }
}
