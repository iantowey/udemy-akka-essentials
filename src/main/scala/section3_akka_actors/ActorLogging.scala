package section3_akka_actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App{

  /**
   * Method 1 explicit logging
   */

  class SimpleActorWithExplicitLogging extends Actor{

    val logger = Logging(context.system, this)

    override def receive: Receive = {
      case message:String => logger.info(message)
    }
  }

  val system = ActorSystem("ActorLoggingDemo")

  val actor = system.actorOf(Props[SimpleActorWithExplicitLogging], "SimpleActorWithExplicitLogging")
  actor ! "blah blah blah"

  /**
   * Method 2 actor with logging
   */
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a:String, i:Int) => log.info("Two Things: '{}' and '{}'", a, i)
      case message:String => log.info(message)
    }
  }

  val actor1 = system.actorOf(Props[ActorWithLogging], "ActorWithLogging")
  actor1 ! "blah blah blah"
  actor1 ! ("blah blah blah", -99)



}
