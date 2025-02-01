package section3_akka_actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import section3_akka_actors.ActorCapabilities.bob
import section3_akka_actors.ActorIntro.{WordCountActor, actorSystem}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.util.Try



object ActorCapabilities extends App{

  class SimpleActor extends Actor{
    override def receive: Receive = {
      case "Hey Hey!!" => context.sender ! s"Hello, there"
      case stringMessage:String => println(s"[${self.path}] I have recieved string $stringMessage")
      case intMessage:Int => println(s"[${self.path}] I have recieved integer $intMessage")
      case longMessage:Long => println(s"[${self.path}] I have recieved long $longMessage")
      case demoClass:DemoClass => println(s"[${self.path}] I have recieved DemoClass ${demoClass.toString}")
      case SendMessageToSelf(content) => self ! content
      case SayHiTo(ref) => ref ! "Hey Hey!!"
      case WirelessPhoneMessage(content, ref) => ref forward s"$content-s" // i keep the original sender of the message
//      case _ => throw new RuntimeException(s"[${self.path}] Unsupported message type")
    }
  }
  val actorSystem = ActorSystem("actorCapabilities")

  val simpleActor = actorSystem.actorOf(Props[SimpleActor].withRouter(RoundRobinPool(10)), "simpleActor")

  val trySendMsgToSimpleActor = trySendMsgToActor(simpleActor)(_)
  // 1. messages can be any type and will be processed as long as it has a matching "case", if message is not matched it is ignored
  // two conditions
  //  a. messages are immutible
  //  b. messages must be serializable (case classes and case objects fit 99.9999% of use cases)
  trySendMsgToSimpleActor("Hello, actor")
  trySendMsgToSimpleActor(5L)
  trySendMsgToSimpleActor(5)
  trySendMsgToSimpleActor(DemoClass("Hello, actor", 5))
  trySendMsgToSimpleActor(UnSupportedDemoClass("Hello, actor", 5))

  // 2. actors have information  about their context and themselves
  // each actor has an implicit attributes called "context", context.self ~ this

  trySendMsgToSimpleActor(SendMessageToSelf("I am an actor and proud"))

//  import scala.concurrent.duration.Duration
//  Await.result(actorSystem.whenTerminated, Duration.apply(10,TimeUnit.SECONDS))

  // 3. actors replying to messages

  val alice = actorSystem.actorOf(Props[SimpleActor], "alice")
  val bob = actorSystem.actorOf(Props[SimpleActor], "bob")

  alice ! SayHiTo(bob) //the null actor is telling the alice actor to send a message to bob

  // 4. Dead letters if there is no sender the message is send to deadletter Q
  alice ! "Hey Hey!!"

  // 5. forwarding messages
  // D -> A -> B
  // forwarding = sending the message with the original sender
  alice ! WirelessPhoneMessage("Hi!", bob)

}

