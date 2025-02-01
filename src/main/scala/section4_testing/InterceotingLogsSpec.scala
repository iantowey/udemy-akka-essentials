package section4_testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

// asserting on log messages is useful integration tests

class InterceptingLogsSpec  extends TestKit(
    ActorSystem("InterceptingLogsSpec",
    ConfigFactory.load("application.conf").getConfig("interceptingLogMessages"))
  )
  with ImplicitSender
  with WordSpecLike //BDD style testing
  with BeforeAndAfterAll {

  //setup
  override def afterAll:Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import InterceptingLogsSpec._

  val item = "Rock the JVM"
  val cc = "1234-1234-1234-1234"

  "A checkout flow" should {
    "correctly log the fulfillment of an order" in {

      //we can test the flow by intercepting the log messages and asserting on the,
      //eventfilter cannot query standard out, need to configure an approiate logger
      //see above config for writing log messages to customer handler -- line 12
      EventFilter.info(pattern = s"Order [0-9]+ for $item has been dispatched", occurrences = 1) intercept {
        val checkoutRef = system.actorOf(Props(checkOutActorFactoru()))
        checkoutRef ! Checkout(item, cc)
      }
    }

    "freakout if payment is denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        val checkoutRef = system.actorOf(Props(checkOutActorFactoru()))
        checkoutRef ! Checkout(item, s"0$cc")
      }
    }
  }
}

//online shop and want to implement a checkout flow

//hard to test this, hard to inject `TestProbes`,
object InterceptingLogsSpec{

  case class Checkout(item:String, creditCard:String)
  case class AuthorizeCard(creditCard:String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(iterm:String)
  case object OrderConfirmed

  val checkOutActorFactoru = {
    () => new Actor with ActorLogging{
      private val paymentManager = context.actorOf(Props(paymentManagerActorFactoru()))
      private val fulFillmentManager = context.actorOf(Props(fulFillmentManagerActorFactoru()))
      override def receive: Receive = awaitingCheckout

      def pendingFulFillment(item: String): Receive = {
        case OrderConfirmed => context.become(awaitingCheckout)
      }

      def pendingPayment(item: String): Receive = {
        case PaymentAccepted => {
          fulFillmentManager ! DispatchOrder(item)
          context.become(pendingFulFillment(item))
        }
        case PaymentDenied => throw new RuntimeException("bailing")
      }

      def awaitingCheckout:Receive ={
        case Checkout(item, creditCard) => {
          paymentManager ! AuthorizeCard(creditCard)
          context.become(pendingPayment(item))
        }
      }
    }
  }

  val paymentManagerActorFactoru = {  //checkoutManager will validate your card here
    () => new Actor with ActorLogging{
      override def receive: Receive = {
        case AuthorizeCard(creditCard) => {
          Thread.sleep(4000) // simulating a delay here, fails for 4s, exceeds the default timeout of 3s,
          // the wait time in the test can be configured via the interceot method on line 34,
          //can update this duration by configuring akka param 'akka.test.filter-leeway'
          sender ! (if(creditCard.startsWith("0"))  PaymentDenied else PaymentAccepted)
        }
      }
    }
  }

  val fulFillmentManagerActorFactoru = { //if payment is accepted checkoutManager will check with this actor
    () => new Actor with ActorLogging{
      var orderId = -1
      override def receive: Receive = {
        case DispatchOrder(item) => {
          orderId += 1
          log.info(s"Order $orderId for $item has been dispatched")
          sender ! OrderConfirmed
        }
      }
    }
  }


}