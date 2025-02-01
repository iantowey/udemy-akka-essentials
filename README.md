
the actor model explained

```
https://www.youtube.com/watch?v=ELwEdb_pD0k
```

TODO --  write a paragraph describign akka architecttre

* actors - trait, etc, starting, stopping
* behaviours - changing, stacking , etc 
* childern
* config
* logging
* testing
    * test probes, using to send receieve messages
    * intercepting logs
    * timed tests
    * synchronous test
* fault tolerance
  * "let it crash" philophy
  * 
* how the testing is BDD
* developing with actors one can easily interpret use case diagrams in a natural way
* developing with actors is the natural mode of interaction humans have with other humans
* .....

#section 1 - intro

essentials (this course - 13 hrs)

persistence (7 hrs)

streams (8.5 hrs)

http (10 hrs)

remoting and clustering (7 hrs)

serialization (3.5 hrs)

#section 2 - scala recap

#section 3 - akka actors

Actors are objects we can't access directly, but only send message to 

* actors extend type Actor
```
type Recieve = PartialFunction[Any, Unit]

trait Actor{
    def recieve:Recieve
}
```
* actors live in a system
```
val actorSystem = ActorSystem("actorSystem")
```

* actors are instanciated in the actor system using the factory method of the actor system
```
val simpleActor = actorSystem.actorOf(Props[SimpleActor], "simpleActor")
```

* in interact with a an actor you  send/recieve messages, actors response in an async manner
```
actor ! "hello"
```

* actors can send messages to themselves, using the `self` keyword
```
self ! "echo"
```

* actors can respond to senders using the `sender` keyword
```
sender ! "this is the reply to your message"
```

Akka syetem has a thread poll which runs all the actors, actor is just a data structure that has an internal queue and message handler

Akka schedules actors for execution in this threadpool

Only one thread may act on an actor at any time (actors are single threaded), no lock needed
the thgread is not released from the ator until the message is finished processing

a message is sent at most once and in order

Actors hold a message handling stack of `Recieve` handlers 

```
a. FOOD(veg)
b. FOOD(veg)
c. FOOD(choco)

handler stack after (a) processed

1. sadRecieve
2. happyRecieve (default handler)

handler stack after (b) processed

1. sadRecieve
2. sadRecieve
3. happyRecieve (default handler)

handler stack after (c) processed

1. sadRecieve
2. happyRecieve (default handler)

```

A trick to figure out how to have the actors stateless is to 
identify all the sections of code where an actors state variables 
a re modified and replace with a `context.become` call with the 
parametertized functions called with new params

Actor Logging is done async

Typesafe hocon properties loading

#### Configuring akka


#section 4 - testing akka actors

* define test suite by extending `TestKit` with the `BDD` style testing scenarios 

* `testActor` is the implicit sender actor in all test classes.

* message have a default timeout of 3 seconds (which can be configured)

* `Testprobes` are special actors with assertion capabilities

* `Testprobes` can reply with messages

* intercepting log messages

* Testing using messages as much as possible, use synchronous tests sparingly


#section 5 - fault tolerance

* `context.stop` stops an actor and child actors

* special messages `PoisonPill|Kill`

####lifecycle

* actor instance : has methods / (may have )internal state

* actorref , 
  * created with `context.actorOf` 
  * has mailbox
  * can recieve messages
  * contains `one` actor instance
  * contains a `uuid`
  * actor path

actors can be
* started
* suspended
* resumed
* restarted (tricker to handle, a new instance of the actor is created, so any state in the old instamnce is lost 
* stopped ()

its fine for actors to crash!

It is up uo the parent to define the stratedgy to manage childs fault tolerance 

When an actor fails its suspends all its childern and sends a signal to its parent via a dedicated message queue
The parent can
* resume the actor (and chilkdern)
* restart the actor and clear its internal state(default)
* stop the actor
* esclate the issue and fail itself

AllforOnestretegy vs OneforOnestrategy

OneforOnestrategy applies the strategy for the failing actors (also takes a number of params retry count in temporal window)

AllforOnestretegy applies the strategy for the all the actors (inclusing the childern)

Failsures are fine, as long as parents have a strategy to deal with failures

Fault tolerance and self healing with the "let it crash" philophasy
#section 6 - akka infrasturcture

#section 7 - akka patterns


##MISC LINKS

https://blog.rockthejvm.com/zio-fibers/?_ga=2.188883819.1385867549.1639154783-591332529.1639154783

https://github.com/rockthejvm

https://github.com/rockthejvm/udemy-akka-essentials

https://github.com/rockthejvm/akka-essentials# udemy-akka-essentials
