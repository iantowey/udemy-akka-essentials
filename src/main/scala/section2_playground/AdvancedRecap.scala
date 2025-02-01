package section2_playground

import scala.concurrent.Future

object AdvancedRecap extends App{

  val pf: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 345
    case _ => 0
  }

  val pf2 = (x:Int) => x match {
    case 1 => 42
    case 2 => 345
    case _ => 0
  }

  val modList = List(1,2,3).map{
    case 1 => 42
    case 2 => 345
    case _ => 0
  }

  //lifting
  val lifted = pf.lift
  assert(lifted(2) == Some(345))


  val pfChain = pf.orElse[Int, Int]{
    case 60 => 89
  }

  //type aliases
  type RecieveFunction = PartialFunction[Any,Unit]

  def recieve:RecieveFunction = {
    case 1 => println("hello")
    case _ => println("confused........")
  }

  //implicits
  implicit val timeout = 3000

  def setTimeout(f:() => Unit)(implicit timeout:Int) = f()

  setTimeout(() => println("timeout"))

  //implicit conversion
  //1 implicit defs
  case class Person(name:String){
    def greet = s"Hi, ny name is $name"
  }
  implicit def fromStringToPen(string:String): Person = Person(string)
  println("Peter".greet)


  //2 implicit class
  implicit class Dog(name:String){
    def bark = println("beark!")
  }
  "Lassie".bark

  //organise
  //local scope
  println(List(1,2,3).sorted)
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _ )
  println(List(1,2,3).sorted)

  //imported scope
  import scala.concurrent.ExecutionContext.Implicits.global
  val fut = Future {
    println("Heloo, future")
  }

  //companion object
  object Person{
    implicit val personOrdering:Ordering[Person] = Ordering.fromLessThan((a,b) => a.name.compareTo(b.name) < 0)
  }
  println(List(Person("Bob"), Person("Alice")).sorted)
}
