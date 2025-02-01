package section7_akka_patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration.DurationInt

/**
 * FSM are an alternative to context.become as changing the behaviour that way can become difficult to debug/read
 *
 * sometimes actors have alot of state and receieve methods which cna make it hard to reason about
 *
 *
 * ... unfinished see video on udemy
 */

class FiniteSTateMachine1Spec
  extends TestKit(ActorSystem("FiniteSTateMachine1Spec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import FiniteSTateMachine1Spec._

  "A FiniteSTateMachine1Spec" should {
    "execute this code" in {
      assert(true)
    }
  }
}


object FiniteSTateMachine1Spec{
  /**
   * Use case
   */
  case class Initialize(
                         inventory:Map[String, Int], // product, product count
                        prices: Map[String, Int]     // product, product count
                       )
  case class RequestProduct(product:String)
  case class Instruction(instruction:String)
  case class RecieveMoney(amount:Int)
  case class Deliver(product:String)
  case class GiveBackChange(amount:Int)
  case class VendingError(reason:String)
  case object RecieveMoneyTimeout

  // implementation in terms of context.become
  class VendingMachineActor extends Actor with ActorLogging{
    import VendingMachineActor._
    override def receive: Receive = idle

    def waitForMoney(
                      inventory: Map[String, Int],
                      prices: Map[String, Int],
                      products:String,
                      money:Int,
                      moneyTimeoutSchedulue:Cancellable,
                      requester:ActorRef
                    ): Receive = {
      case RecieveMoneyTimeout =>
    }

    def startRecieveTimeoutScheduled(): Cancellable = context.system.scheduler.scheduleOnce(1 second){
      self ! RecieveMoneyTimeout
    }(context.dispatcher)

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None | Some(0) => sender ! VendingError(ProductNotAvavilable)
        case Some(_) => {
          val price  = prices(product)
          sender ! Instruction(s"Please insert $price dollars")
          context.become(waitForMoney(inventory, prices, product, price, startRecieveTimeoutScheduled(), sender))
        }
      }
    }

    def idle:Receive = {
      case Initialize(inventory, prices) => context.become(operational(inventory, prices))
      case _ => VendingError(MachineNotInitialized)
    }
  }
  object VendingMachineActor{
    val MachineNotInitialized = "MachineNotInitialized"
    val ProductNotAvavilable = "ProductNotAvavilable"
  }
}