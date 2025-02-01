package section5_faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}
import section5_faulttolerance.StartingStoppingActors.Parent.StartChild

object StartingStoppingActors extends App{

  val system = ActorSystem("StartingStoppingActors")

  object Parent{
    case class StartChild(name:String)
    case class StopChild(name:String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildern(Map())

    def withChildern(childern:Map[String, ActorRef]): Receive = {
      case StartChild(name) => {
        log.info(s"Starting child $name")
        context.become(withChildern(childern + (name -> context.actorOf(Props[Child], name))))
      }
      case StopChild(name) => {
        log.info(s"Stopping child with the name $name")
        childern.get(name).foreach(context.stop(_))
      }
      case Stop => {
        log.info("parent going to stop, this will stop all childern")
        context.stop(self)
      }
      case message:String => log.info(message)
    }
  }

  class Child extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * 1. context.stop
   */
//  import Parent._
//  val parent = system.actorOf(Props[Parent], "ian")
//  parent ! StartChild("alice")
//  val alice = system.actorSelection("/user/ian/alice")
//  alice ! "Hello, World"
//  parent ! StopChild("alice")
//  (0 to 50).foreach(_ => alice ! "are you still awake?") // child still processes a few message before the stop call gets to it
//
//  parent ! StartChild("aaron")
//  val aaron = system.actorSelection("/user/ian/aaron")
//  aaron ! "Hello, World"
//  parent ! "Hello Me"
//  parent ! Stop
//  (0 to 10).foreach(i => parent ! s"[$i]parent, are you still there ?") // parent still processes a few message before the stop call gets to it
//  (0 to 100).foreach(i => aaron ! s"[$i]aaron, are you still there ?") // child still processes a few message before the stop call gets to it

  /**
   * 2. special messages
   */

//  val looseActor = system.actorOf(Props[Child], "looseActor")
//  looseActor ! "hello, loose actor"
//  looseActor ! PoisonPill // this will stop self
//  looseActor ! "are you still there?"

//  val termActor = system.actorOf(Props[Child], "termActor")
//  termActor ! "goodbye"
//  termActor ! Kill // this will stop self
//  termActor ! "are you still there?"

  /**
   * 3. death watch
   */

  val watcherFactory = () => new Actor with ActorLogging{
    import Parent._
    override def receive: Receive = {
      case StartChild(name)=> {
        val child = context.actorOf(Props[Child], name)
        log.info(s"starting and watching child $name")
        context.watch(child) // ** registers this actor for the death of the child, when the child dies this actor will recieve a message "Terminated" with the ref of the chikd
      }
      case Terminated(ref) => log.info(s"the ref that i'm watching ${ref.path} has been stopped")
    }
  }

  val watcher = system.actorOf(Props(watcherFactory()), "watcher")
  watcher ! StartChild("c1")
  val c1 = system.actorSelection("/user/watcher/c1")
  Thread.sleep(500)
  c1 ! PoisonPill
}
