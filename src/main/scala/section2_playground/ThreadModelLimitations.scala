package section2_playground

import scala.concurrent.Future

object ThreadModelLimitations extends App{

  //current thread model issues
  /*
  1. OOP encapsuluation is only valid in the SINGLE THREADED MODEL,

   */
  //no thread safe
//  class BankAccount(private var amt:Int){
//    override def toString: String = super.toString
//
//    //not good practive "sync block
//    def withDraw(withAmt:Int)  = this.amt -= withAmt
//    //not good practive "sync block
//    def deposit(withAmt:Int)  = this.amt += withAmt
//
//    def getAmt = amt
//  }
//
//  val ba1 = new BankAccount(2000)
//
//  for(_ <- 1 to 1000){
//    new Thread(() => ba1.withDraw(1)).start
//  }
//
//  for(_ <- 1 to 1000){
//    new Thread(() => ba1.deposit(1)).start
//  }
//
//  println(ba1.getAmt)
//
//  //OOP encapsuluation breaks down in multithreaded envs
//  //synchronization, locks to the rescue
//
//  class BankAccountSync(private var amt:Int){
//    override def toString: String = super.toString
//
//    //not good practive "sync block
//    def withDraw(withAmt:Int)  = this.synchronized{this.amt -= withAmt}
//    //not good practive "sync block
//    def deposit(withAmt:Int)  = this.synchronized{this.amt += withAmt}
//
//    def getAmt = amt
//  }
//
//  val ba2 = new BankAccountSync(2000)
//
//  for(_ <- 1 to 1000){
//    new Thread(() => ba2.withDraw(1)).start
//  }
//
//  for(_ <- 1 to 1000){
//    new Thread(() => ba2.deposit(1)).start
//  }
//
//  println(ba1.getAmt)
//
  //this introduces more issues, deadlocks,likelocks

  /*
  2. delegating something to a thread is a pain
   */

  //you have a running htread and you want to pass a runnable to that thread, how ?
  var task:Runnable = null
  val t:Thread = new Thread(() => {
    while(true){
      while (task == null) {
        t.synchronized {
          println("[background] witing for task to be assigned")
          t.wait()
        }
      }
      task.synchronized{
        println("[background] I have a task")
        task.run
        task = null
      }
    }
  })

  def delegateToBackgrounfThread(r:Runnable) = {
    if(task == null) task = r

    t.synchronized{
      t.notify
    }
  }

  t.start
  Thread.sleep(1000)
  delegateToBackgrounfThread(() => println(42))
  Thread.sleep(1000)
  delegateToBackgrounfThread(() => println("this should run in the background"))

  /*
  3. tracing and dealing with errors in a multi threaded env is hard!
   */
  import scala.concurrent.ExecutionContext.Implicits.global

  val futs = (0 to 9)
    .map(i => 100000*i until 100000*(i+1))  // 0 - 99,000; 100,000 - 199,000
    .map(rng => Future{
      if(rng.contains(546735)) throw new RuntimeException("Invalid Number")
      rng.sum
    })

  val sumFut = Future.reduceLeft(futs)(_+_)
  sumFut.onComplete(println)

}
