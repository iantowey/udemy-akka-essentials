package section5_faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.Future
import scala.util.Random

class DispatchersActors extends TestKit(ActorSystem(
    "BackoffSupervisorPattern"))//,
//    ConfigFactory.load().getConfig("dispatchers-demo")))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import DispatchersActors._

  /**
   * Dispatcher controls how messages are being sent and handled
   */

  "A DispatchersActors" should {
    "method1 : programatic attached dispatcher " in {

      /**
       * see defn of "my-dispatcher" in application.conf
       *
       * even thought we created 10 slaves,only 3 threads will be scheduled by the dispatcher at any one time
       * and the throughput is 30, which means the dispatcher will process 30 messages for an actor before the
       * dispatcher moves onto another actor
       *
       * this is shown in the logs below, changing fixed-pool-size = 1 line 59 of application.conf, you see in the
       * logs 30 messages being processed by the same actor before
       * the single thread in the dispatcher pool moves away to the next actor who will also process 30 messages
       */

      val simpleCounters = (1 to 10).map(i => system.actorOf(Props[CounterActor].withDispatcher("my-dispatcher"), s"counter_$i"))

      val r = new Random
      (1 to 1000).foreach(i => {
        simpleCounters(r.nextInt(10)) ! i
      })
      Thread.sleep(5000)
    }

    "method 2 attached to dispather in config" in {
      val rtjvm = system.actorOf(Props[CounterActor], "rtjvm")
      (1 to 1000).foreach(i => {
        rtjvm ! i
      })
      Thread.sleep(5000)
    }

    /**
     * Dispatchers implement the ExecutionCOntext trait
     */
    "run future from actor [blocking]" in {
      val dbActor = system.actorOf(Props[DBActor])
      dbActor ! "the meaning of life is 42"
      Thread.sleep(10000)
    }
    "[nonblocking vs blocking]" in {
      val simpleCounters = system.actorOf(Props[CounterActor], s"counter")
      val dbActor = system.actorOf(Props[DBActor], "dbActor")
      (1 to 1000).foreach(i => {
        simpleCounters  ! s"important message $i"
        dbActor ! s"important message $i"  //the future is starving the dispatcher pool of resources
        // use a dedicated dispatcher
      })
      Thread.sleep(10000)
    }
  }
}


object DispatchersActors{

  class DBActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => {
        Future {
          Thread.sleep(5000)
          log.info(s"success $message")
          //        }(context.dispatcher) // solution 1 : dont do this, it will starve the dispatcher thread pool of resources
        }(context.system.dispatchers.lookup("my-dispatcher")) // solution 2 : custom dispatcher from application.conf, this will not block the actors receieving messages
//      } // solution 3: use a router!
      }
    }
  }

  class CounterActor extends Actor with ActorLogging{
    var count = 0
    override def receive: Receive = {
      case message => {
        count+=1
        log.info(s"${message.toString} #$count")
      }
    }
  }
}
