package section3_akka_actors.exercise

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercise_16 extends App {

  /**
   * Exercise
   *
   * Create a actor hierarchy, common "probkem distributuion of work"
   *
   * master -> *executors
   *
   * master - moniters and distributes work to the executors
   * executor - executes a subset of the workload
   */

  object WordCountMaster{
    case class Init(nChildern:Int)
    case class WordCounterTask(id:Int, text:String)
    case class WordCounterReply(id:Int, count:Int)
    case object RESULT
  }

  class WordCountMaster(maxWorkerTaskStringLen:Int) extends Actor{
    import WordCountMaster._

    override def receive: Receive = init

    def init : Receive = {
      case Init(nChildern) => context.become(map((0 to nChildern-1).map(i => context.actorOf(Props[WordCountWorker],s"worker_$i"))), true)
    }

    def map(workers:Seq[ActorRef]):Receive = {
      case s:String => {
        val tasks = s.sliding(math.min(maxWorkerTaskStringLen, s.length),math.min(maxWorkerTaskStringLen, s.length)).toArray
        tasks
          .zipWithIndex
          .foreach{
            case (partialString, idx) => workers(idx % workers.size) ! WordCounterTask(idx, partialString)
          }
        context.become(reduce(tasks.size, 0))
      }
    }

    def reduce(outstandingTasks:Int, wordCountTotal:Int):Receive = {
      case WordCounterReply(_, wc) => {
        context.become(reduce(outstandingTasks-1, wordCountTotal+wc))
      }
      case RESULT => {
        println(s"the total number of words is $wordCountTotal, $outstandingTasks tasks remaining")
        if(outstandingTasks > 0){
          self ! RESULT
        }
      }
    }
  }

  class WordCountWorker extends Actor{
    import WordCountMaster._
    override def receive: Receive = {
      case WordCounterTask(id,str) => {
        println(s"[${self.path}] ... ")
        context.sender ! WordCounterReply(id, str.split(" ").length)
      }
    }
  }

  val actorSystem = ActorSystem("ChildActorsExercise_16")
  val master = actorSystem.actorOf(Props(new WordCountMaster(1000)))

  //tasks will be send to workers in a round robin fashion
  import WordCountMaster._
  master ! Init(10)
  master ! "this is a test string "*1000
  master ! RESULT

}
