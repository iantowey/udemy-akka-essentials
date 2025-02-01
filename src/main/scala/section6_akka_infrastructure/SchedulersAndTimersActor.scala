package section6_akka_infrastructure

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SchedulersAndTimersActor
  extends TestKit(ActorSystem("SchedulersAndTimersActor", ConfigFactory.load()))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import SchedulersAndTimersActor._

  import system.dispatcher

  "A SchedulersAndTimersActor demo" should {

    "execute this code once on a schedule" in {
      val sa = system.actorOf(Props[SimpleActor], "sa")
      system.log.info("Scheduling reminder for simple actor")
      system.scheduler.scheduleOnce(1 second){
        sa ! "reminder"
      }
      Thread.sleep(10000)
    }

    "execute this code once on a interval and cancel after 4 seconds" in {
      val sa = system.actorOf(Props[SimpleActor], "sa")
      system.log.info("Scheduling reminder for simple actor")
      val routine = system.scheduler.scheduleAtFixedRate(1 second, 2 seconds){
        () =>sa ! "reminder"
      }

      system.scheduler.scheduleOnce(4 seconds){
        routine.cancel()
      }
      Thread.sleep(10000)
    }

    "exercise - implement a self closing actor" in {
      /*
       if the actor recieves a message (anything), you have 1 second to send it another message
       if the time window expires, the actor will stop itself
       if you send another message, the time window is reset
       */
      val sca = system.actorOf(Props(new SelfClosingActor2(system.dispatcher)), "sca")
//      val routine = system.scheduler.scheduleAtFixedRate(500 millis, 1 seconds){
//        () => sca ! "reminder"
//      }
      system.scheduler.scheduleOnce(250 millis){
        sca ! "ping"
      }
      system.scheduler.scheduleOnce(2 seconds){
        sca ! "pong"
      }
      Thread.sleep(10000)
    }

    "akka util for sending message to itself - timer" in {
      val timerActor = system.actorOf(Props[TimerBasedSCAActor], "timerActor")
      system.scheduler.scheduleOnce(5 seconds){
        timerActor ! Stop
      }

      Thread.sleep(10000)
    }
  }
}


object SchedulersAndTimersActor{

  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }


  case object TimerKey
  case object Start
  case object Reminder
  class TimerBasedSCAActor extends Actor with ActorLogging with Timers{

    timers.startSingleTimer(TimerKey,Start,500 millis)

    override def receive: Receive = {
      case Start => {
        log.info("Bootstrapping......")
        timers.startTimerAtFixedRate(TimerKey, Reminder, 1 second)
      }
      case Reminder => {
        log.info("I'm alive")
      }
      case Stop => {
        log.warning("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)
      }
      case message => log.info(message.toString)
    }
  }

  class SelfClosingActor2(exeContext:ExecutionContext) extends Actor with ActorLogging {
    var schedule = createTimeo0utWindow()
    def createTimeo0utWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second){
        self ! "timeout"
      }(exeContext)
    }

    override def receive: Receive = {
      case "timeout" => {
        log.info("stopping myself")
        context.stop(self)
      }
      case message => {
        log.info(s"Recieved $message, staying alive")
        schedule.cancel
        schedule = createTimeo0utWindow
      }
    }
  }

  /**
   * exercise attempt
   * @param exeContext
   */
  class SelfClosingActor(exeContext:ExecutionContext) extends Actor with ActorLogging{
    var timer:Long = System.currentTimeMillis

    override def preStart(): Unit =  {
      context.system.scheduler.scheduleAtFixedRate(1 seconds, 2 seconds){
        () =>
          if((System.currentTimeMillis - timer) > 1000) {
            log.info(s"waitng too long for a message stopping....${System.currentTimeMillis - timer}")
            context.self ! Stop
          } else {
            log.info(s"got heartbeat within interval....${System.currentTimeMillis - timer}")
            timer = System.currentTimeMillis
          }
      }(exeContext)
    }

    override def receive: Receive = {
      case message => {
        timer  = System.currentTimeMillis
        log.info(message.toString)
      }
    }
  }
}