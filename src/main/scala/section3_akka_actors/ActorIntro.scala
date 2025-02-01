package section3_akka_actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorIntro extends App{

  // part 1 : actor system
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // part 2 : create actors in the actor system
  // think of actors like people, you send a message (talk, text,call,email) and wait for response asynchroncously
  // each actor may respond differently

  //word count actor - he counts words
  class WordCountActor extends Actor{
    var totalWords:Int = 0
    //actor behaviour - count words in message
    override def receive: Receive = {
      case message:String => {
        println(s"I have recieved '$message'")
        totalWords += message.split(" ").size
      }
      case msg => println(s"[word counter] I dont understand ${msg.toString}")
    }
  }

  //part 3 - instanciate actor
  // actors can only be instanciated via the 'actorOf' factory method
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val diffWordCounter = actorSystem.actorOf(Props[WordCountActor], "diffWordCounter")

  // part 4 - communicate ith actor
  wordCounter ! "i am learning akka, and its cool!"
  diffWordCounter  ! "A different message"

  // part 5 : actor with constructor args
  class Person(name:String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }

  val person = actorSystem.actorOf(Props(new Person("Bob"))) // but not good practice
  person ! "hi"

  //instead create a companion object
  class Person2(name:String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }

  object Person2{
    def props(name:String) = Props(new Person(name))
  }

  val person2 = actorSystem.actorOf(Person2.props("Bob2")) // but not good practice
  person2 ! "hi"

}
