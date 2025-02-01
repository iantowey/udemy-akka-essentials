package section7_akka_patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import section7_akka_patterns.AskMessagePatternSpec.AuthManager.{AUTH_FAILURE_INCORRECT_PASSWORD, AUTH_FAILURE_NOT_FOUND, AUTH_FAILURE_SYSTEM_ERR}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class AskMessagePatternSpec
  extends TestKit(ActorSystem("AskMessagePatternSpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import AskMessagePatternSpec._

  def runAuthSuite(props:Props) ={

    val authManager = system.actorOf(props)

    "fail to auth unknown user" in {
      authManager ! Authenticate("ian", "pass")
      expectMsg(AuthFail(AUTH_FAILURE_NOT_FOUND))
    }
    "fail to auth an invalid password" in {
      authManager ! RegUser("ian", "pass")
      authManager ! Authenticate("ian", "pass11")
      expectMsg(AuthFail(AUTH_FAILURE_INCORRECT_PASSWORD))
    }
    "success auth " in {
      authManager ! RegUser("ian", "pass")
      authManager ! Authenticate("ian", "pass")
      expectMsg(AuthSuccess)
    }

  }

  "An authenticator using ask" should {
    runAuthSuite(Props[AuthManager])
  }

  "An authenticator using pipe" should {
    runAuthSuite(Props[PipedAuthManager])
  }

}


object AskMessagePatternSpec{
  //assume this code is someewhere else in your app
  // UserAuth Demo
  case class Read(key:String)
  case class Write(key:String, value:String)
  class KVActor extends Actor with ActorLogging{
    override def receive: Receive = online(Map())

    def online(kv:Map[String,String]):Receive ={
      case Read(key) => {
        log.info(s"trying to read the value at the key $key")
        sender ! kv.get(key)
      }
      case Write(k,v) => {
        log.info(s"writing the value $v for key $k")
        context.become(online(kv + (k -> v)))
      }
    }
  }

  case class RegUser(username:String, password:String)
  case class Authenticate(username:String, password:String)
  case class AuthFail(message:String)
  case object AuthSuccess

  object AuthManager{
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_INCORRECT_PASSWORD = "incorrect password"
    val AUTH_FAILURE_SYSTEM_ERR = "sys error"
  }

  class AuthManager extends Actor with ActorLogging{
    protected val authDB = context.actorOf(Props[KVActor], "KVActor")
    import AuthManager._
    import akka.pattern.ask
    implicit val timeout:Timeout =  Timeout(1 second)
    implicit val ex:ExecutionContext = context.dispatcher
    override def receive: Receive = {
      case RegUser(username, password) => authDB ! Write(username, password)
      case Authenticate(username, password) => handleAuth(username, password)
    }

    def handleAuth(username:String, password:String) = {
      val originalSender = sender
      val future = authDB ? Read(username) // returns a future
      future.onComplete{
        // **** NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE
        // avoid "closing over" the actor instance or the mutuible state
        // dont use "sender"
        case Success(None) => originalSender ! AuthFail(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(pw)) =>  originalSender ! (if (password == pw) AuthSuccess else AuthFail(AUTH_FAILURE_INCORRECT_PASSWORD))
        case Failure(_) => originalSender ! AuthFail(AUTH_FAILURE_SYSTEM_ERR)
      }
    }
  }

  class PipedAuthManager extends AuthManager{
    import akka.pattern.{ask, pipe}

    override def handleAuth(username: String, password: String): Unit = {
      val future = authDB ? Read(username)
      val passwordFuture = future.mapTo[Option[String]]
      val repsFuture = passwordFuture.map{
        case None => AuthFail(AUTH_FAILURE_NOT_FOUND)
        case Some(pw) =>  (if (password == pw) AuthSuccess else AuthFail(AUTH_FAILURE_INCORRECT_PASSWORD))
        case _ =>  AuthFail(AUTH_FAILURE_SYSTEM_ERR)
      }

      /**
       * When the future completes, send te response to the actor ref in the arg list
       */
      repsFuture.pipeTo(sender)
    }
  }
}