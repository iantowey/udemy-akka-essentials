package section6_akka_infrastructure

import akka.actor.{Actor, ActorLogging, ActorPath, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, BroadcastRoutingLogic, ConsistentHashingRoutingLogic, FromConfig, RandomRoutingLogic, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router, ScatterGatherFirstCompletedRoutingLogic, SmallestMailboxRoutingLogic, TailChoppingRoutingLogic}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import section3_akka_actors.IntroAkkaConfig.config

/**
 * Routers
 *  usefult when you wanbt to spread work amoung actors of the same kind
 *
 *  routers (think of traditions app services) are middle actors they pass work to more fine grained worker actors
 */

class RouterActors
  extends TestKit(ActorSystem("RouterActors", ConfigFactory.load().getConfig("routers-demo")))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import RouterActors._

  "A RouterActors" should {
    "pass code to slaves" in {
      val router = system.actorOf(Props[Master], "master")
      (0 to 10).foreach{i => router ! s"msg_$i hello master"}
      Thread.sleep(5000)
    }
    "method 2.1 : does the same thing as complicated master above" in {
      /**
       * 2 router actors with its own childern
       */
      val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "poolMaster")
      (0 to 10).foreach{i => poolMaster ! s"msg_$i hello master"}
      Thread.sleep(5000)
    }
    "method 2.2 : from configuration" in {
      /**
       * 2 router actors with its own childern fro config: seection "routers-demo" in application.conf
       */
      val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
      (0 to 10).foreach{i => poolMaster2 ! s"msg_$i hello master"}
      Thread.sleep(5000)
    }
    "method 3.1 : router with actors created elsewhere: GROUP router" in {
      /// .... somehwre in app
      val (slaveList, slavePaths) = (1 to 5).map(i => {
        val slave = system.actorOf(Props[Slave], s"slave_$i")
        (slave, slave.path.toString)
      }).unzip

      val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
      (0 to 10).foreach{i => groupMaster ! s"msg_$i hello master"}
      Thread.sleep(5000)

    }
    "method 3.2 : from config" in {
      val (slaveList, slavePaths) = (1 to 5).map(i => {
        val slave = system.actorOf(Props[Slave], s"slave_$i")
        (slave, slave.path.toString)
      }).unzip

      val groupMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "groupMaster2")
      (0 to 10).foreach{i => groupMaster2 ! s"msg_$i hello master"}
      Thread.sleep(5000)

    }
    "handling of special message" in {
      val (slaveList, slavePaths) = (1 to 5).map(i => {
        val slave = system.actorOf(Props[Slave], s"slave_$i")
        (slave, slave.path.toString)
      }).unzip

      val groupMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "groupMaster2")
      groupMaster2 ! Broadcast("Hello, all")
      Thread.sleep(5000)

      //PoisonPill and Kill are NOT routed, they are handled by the routing actor
      //AddRoutee, RemoveRoutee, GetRoutee, handled only by the routing actor

      //Recap
//      # method 1 : dont use this
//      # method 2 : pool routers
//      # method 3 : group routers
    }
  }
}


object RouterActors{

  /**
   * master slave hierarchy, master will route all the requests to child actors
   */

  class Master extends Actor with ActorLogging{
    /*
    how to route to slaves? a few ways if doing this

      1. Manual Routing
        5 actor routees based off Slave actors
     */
    // 1 create routees
    private val slaves = (1 to 5).map(i => {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    })

    // 2 create router
    private val router = Router(
      RoundRobinRoutingLogic(),
//      RandomRoutingLogic(), //dont use this
//      SmallestMailboxRoutingLogic(), //sends message to actor with smalest mailbox
//      BroadcastRoutingLogic(), //sends to all actors
//      ScatterGatherFirstCompletedRoutingLogic(), // broadcasts and gathers the first response next replies are diguarded
//      TailChoppingRoutingLogic() // forwards next messages to each slave sequentally and sopts when the first reply is recieved
//        ConsistentHashingRoutingLogic(), //all messages with the same hash get to the same actor
      slaves)

    override def receive: Receive = {
        // 3 route messages
      case message => router.route(message, sender) // slaves can reply directly to the sender
        // 4 handle termination/lifecycle of the routes
      case Terminated(ref) => {
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
      }
    }
  }

  class Slave extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
}