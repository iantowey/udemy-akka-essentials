package section5_faulttolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import java.io.File
import scala.concurrent.duration.DurationInt
import scala.io.Source

class BackoffSupervisorPattern extends TestKit(ActorSystem("BackoffSupervisorPattern")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll{

  //Backoff recap
//    * backoff supervision, actor is wrapped in a supervisor which controls how it is created after failure


  //sometimes restarting apps directly can cause more harm than good
  //ads in exponential delays and randomness into the restart/resume/stop etc of child actors

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import BackoffSupervisorPattern._

  "A FileBasedPersistentActor" should {
    "readFile ok if it exists" in {
      val actor =  system.actorOf(Props[FileBasedPersistentActor], "FileBasedPersistentActor0")
      actor ! ReadFile("important.txt")
    }
    "fail and restart if file does not exist " in {
      val actor =  system.actorOf(Props[FileBasedPersistentActor], "FileBasedPersistentActor1")
      actor ! ReadFile("data.txt")
    }
    // if source is a shared resource such as a db connection, instantly restarying my not be the best idea, a backoff strategy might be better
    // to give the resource time to get backonline
    "backoff gracefully when fails and restarts" in {
      val simpleSupervisorProps = BackoffSupervisor.props(
        BackoffOpts.onFailure(
        Props[FileBasedPersistentActor],
        "FileBasedPersistentActor3",
        3 seconds, // 3s, 6s, 12s, 24s,
        30 seconds,
          0.2 // adds some random noise to the above timings
      ))

      val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisorProps")
//      simpleBackoffSupervisor ! ReadFile("important.txt")
      simpleBackoffSupervisor ! ReadFile("data.txt")
      Thread.sleep(10000)
      /*
      simpleSupervisorProps
        - child called simplebackoffactor (props of type  FileBasedPersistentActor)
        - supervisoion strategy is the default one (restart everything)
          - first attempt after 3 seconds
          - next attempt is 2x the prev attaemot
       */
    }
    "stop supervisor" in {
      val stopSupervisorProps = BackoffSupervisor.props(
        BackoffOpts.onStop(
          Props[FileBasedPersistentActor],
          "stopBackoffActor",
          3 seconds, // 3s, 6s, 12s, 24s,
          30 seconds,
          0.2 // adds some random noise to the above timings
        ).withSupervisorStrategy{
          OneForOneStrategy(){
            case _ => Stop
          }
        })

      val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
      stopSupervisor  ! ReadFile("data.txt")
      Thread.sleep(10000)
    }

    "EagerFileBasedPersistentActor demo" in {
      val eagerFileBasedPersistentActor = system.actorOf(Props[EagerFileBasedPersistentActor])
      eagerFileBasedPersistentActor ! ReadFile("important.txt")
      //ActorInitializationException (default strategy is  )=> STOP
    }

    "repeated supervisor" in {
      val repeatedSupervisorProps = BackoffSupervisor.props(
        BackoffOpts.onStop(
          Props[EagerFileBasedPersistentActor],
          "stopBackoffActor",
          1 seconds, // 3s, 6s, 12s, 24s,
          60 seconds,
          0.1 // adds some random noise to the above timings
        ).withSupervisorStrategy{
          OneForOneStrategy(){
            case _ => Stop
          }
        })
      val repeatedSupervisorActor = system.actorOf(repeatedSupervisorProps, "repeatedSupervisorActor")
      repeatedSupervisorActor  ! ReadFile("")


//      val repeatedBackoffSupervisor = system.actorOf(repeatedSupervisorProps, "repeatedSupervisorProps")
//      repeatedBackoffSupervisor  ! ReadFile("data.txt")
      Thread.sleep(10000)
      repeatedSupervisorActor  ! ReadFile("")
    }
  }
}

object BackoffSupervisorPattern{
  case class ReadFile(fileName:String)
  class FileBasedPersistentActor extends Actor with ActorLogging{
    var dataSource: Source = null

    override def preStart(): Unit = {
      log.info("Persistent actor starting")
    }

    override def postStop(): Unit = {
      log.info("Persistent actor has stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info("Persistent actor restarting")
    }

    override def receive: Receive = {
      case ReadFile(fileName:String) => {
        if(dataSource == null) dataSource = Source.fromFile(new File(s"src/main/resources/testfiles/${fileName}"))
        log.info("ive read important data: " + dataSource.getLines().toList.mkString(" "))
      }
    }
  }

  class EagerFileBasedPersistentActor extends FileBasedPersistentActor{
    override def preStart(): Unit = {
      log.info("EagerFileBasedPersistentActor starting....")
      dataSource = Source.fromFile(new File(s"src/main/resources/testfiles/dummy.txt"))
    }
  }
}