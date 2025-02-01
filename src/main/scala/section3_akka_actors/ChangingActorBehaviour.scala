package section3_akka_actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import section3_akka_actors.ActorCapabilities.SimpleActor
import section3_akka_actors.ChangingActorBehaviour.Mom.MomStart

object ChangingActorBehaviour extends App{

  object FussyKid{
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor{
    import FussyKid._
    import Mom._
    var state = HAPPY
    override def receive: Receive = {
      case Food(VEG) => state = SAD
      case Food(CHOCO) => state = HAPPY
      case Ask(_) =>
        if(state == HAPPY){
          context.sender ! KidAccept
        } else {
          context.sender ! KidReject
        }
    }
  }

  object Mom {
    case class MomStart(kidRef:ActorRef)
    case class Food(food: String)
    case class Ask(msg:String)
    val VEG = "veggies"
    val CHOCO = "choco"
  }

  class Mom extends Actor{
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(ref) => { //kid actor will recieve in the correct order as listed below
        ref ! Food(VEG)
        ref ! Ask("Do you want to play")
        ref ! Food(CHOCO)
        ref ! Ask("Do you want to play")
        ref ! Food(CHOCO)
        ref ! Ask("Do you want to play")
        ref ! Food(VEG)
        ref ! Ask("Do you want to play")

      }
      case KidAccept => println("Yay, my kid is happy")
      case KidReject => println("My kid is sad (but healthy)")
    }
  }

  val actorSystem = ActorSystem("changingActorBehaviour")

  val mom = actorSystem.actorOf(Props[Mom], "mom")
  val kid = actorSystem.actorOf(Props[FussyKid], "kid")

  mom  ! MomStart(kid)

  class StatelessFussyKid extends Actor{
    import Mom._
    import FussyKid._
    override def receive: Receive = happyRecieve //default behaviour

    def happyRecieve: Receive = {
      case Food(VEG) => context.become(sadRecieve, false)//change handler to sadRecieve, will be used for all future message until changed
      case Food(CHOCO) => //stay happy
      case Ask(_) => context.sender ! KidAccept
    }
    def sadRecieve: Receive = {
      case Food(VEG) => context.become(sadRecieve, false)
      case Food(CHOCO) => context.unbecome()
      case Ask(_) => context.sender ! KidReject
    }
  }

  val statelessKid = actorSystem.actorOf(Props[StatelessFussyKid], "statelessKid")

  mom ! MomStart(statelessKid)

}