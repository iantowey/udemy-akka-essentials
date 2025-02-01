package section3_akka_actors.exercise

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorBehaviourExercise_14 extends App {

  val actorSystem = ActorSystem("ActorBehaviourExercise_14")

  /**
   * Exercises
   *
   * 1. recreate the counter actor with context.become and no mutable state
   */

  //DOMAIN of the counter (message that the actor supports, put in companion object)
  object CounterActor {
    case object INCREMENT

    case object DECREMENT

    case object PRINT
  }

  class CounterActor extends Actor {

    import CounterActor._

    override def receive: Receive = countRecieve(0)

    def countRecieve(currentCount: Int): Receive = {
      case INCREMENT => context.become(countRecieve(currentCount + 1), true)
      case DECREMENT => context.become(countRecieve(currentCount - 1), true)
      case PRINT => println(s"[${self.path}] ${currentCount}")
    }
  }

  val counterActor = actorSystem.actorOf(Props[CounterActor], "counterActor")

  (1 to 10).foreach { _ => counterActor ! CounterActor.INCREMENT }
  counterActor ! CounterActor.PRINT
  (1 to 5).foreach { _ => counterActor ! CounterActor.DECREMENT }
  counterActor ! CounterActor.PRINT

  /**
   * Exercises
   *
   * 2. simplified voting system :
   *
   */

  case class Vote(candidate: String)

  case object VoteStatusRequest

  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {
    override def receive: Receive = notVoted

    def notVoted: Receive = {
      case VoteStatusRequest => context.sender ! VoteStatusReply(None)
      case Vote(candidate) => context.become(hasVoted(candidate), true)
    }

    def hasVoted(candidate: String): Receive = {
      case VoteStatusRequest => context.sender ! VoteStatusReply(Some(candidate))
    }

  }

  // once voted , state is changed to voted and cant vote again
  case class AggVotes(citizens: Set[ActorRef])

  case object ElectionResult

  case class CheckAllVotesCounted(citizens: Map[ActorRef, Int])

  class VoteAgger(maxCheckCount:Int = 10) extends Actor {

    override def receive: Receive = initElectionCount

    def initElectionCount: Receive = {
      case AggVotes(set) => {
        println("all votes have been cast, changing behaviour to counting")
        context.become(countVotes(Map(), set.map(_ -> 0).toMap), true)
        set.foreach(_ ! VoteStatusRequest)
      }
    }

    def countVotes(runningElectionCount: Map[String, Int], electorateWithCheckCount: Map[ActorRef, Int]): Receive = {
      case VoteStatusReply(None) => {
        println("Asking again")
        context.sender ! VoteStatusRequest
      }
      case VoteStatusReply(optCandidate) => {
        println(s"${sender.path} voted for ${optCandidate} ")
        val toProcess = electorateWithCheckCount - sender
        context.become(countVotes(runningElectionCount + (optCandidate.get -> (runningElectionCount.get(optCandidate.get).getOrElse(0) + 1)), toProcess))
        self ! CheckAllVotesCounted(toProcess)
      }
      case CheckAllVotesCounted(citizens) if citizens.size > 0 => {
        println(s"all votes have not been counted,${citizens.size} to process. checking again (checking a max of 10 ten times per citizen)")
        self ! CheckAllVotesCounted(citizens.map { case (k, v) => k -> (v + 1) }.filter(_._2 <= maxCheckCount))
      }
      case CheckAllVotesCounted(citizens) if citizens.size == 0 => {
        println("all votes have been counted, changing behaviour to result")
        context.become(electionResult(runningElectionCount), true)
        self ! ElectionResult
      }
    }

    def electionResult(m: Map[String, Int]): Receive = {
      case ElectionResult => {
        println("**************************************************")
        println("Election result.....")
        println("**************************************************")
        val s = m.map { case (k, v) => s"* $k -> $v\n" }.mkString("")
        println("**************************************************")
        println(s)
      }
    }
  }

  val alice = actorSystem.actorOf(Props[Citizen])

  val a1 = actorSystem.actorOf(Props[Citizen])
  val a2 = actorSystem.actorOf(Props[Citizen])
  val a3 = actorSystem.actorOf(Props[Citizen])
  val a4 = actorSystem.actorOf(Props[Citizen])
  val a5 = actorSystem.actorOf(Props[Citizen])

  a1 ! Vote("Martin")
  a2 ! Vote("Jonas")
  a3 ! Vote("Roland")
  a4 ! Vote("Roland")

  val voteAgger = actorSystem.actorOf(Props(new VoteAgger(10)))
  voteAgger ! AggVotes(Set(a1, a2, a3, a4, a5))

  /**
   * print the status of the votes
   *
   * Martin -> 1
   * Jonas -> 1
   * Roland -> 2
   *
   */
}
