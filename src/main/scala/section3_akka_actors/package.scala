import akka.actor.ActorRef

import scala.util.Try

package object section3_akka_actors {

  case class DemoClass(s:String, i:Int)
  case class UnSupportedDemoClass(s:String, i:Int)
  case class SendMessageToSelf(content:String)
  case class SayHiTo(ref:ActorRef)
  case class GoNuts(ref:ActorRef)
  case class WirelessPhoneMessage(content:String, ref:ActorRef)

  def trySendMsgToActor(actor: ActorRef)(msg: Any): Unit = {
    Try {actor ! msg}.recover { case t => print(s"Error sending message to actor $t") }
  }

}
