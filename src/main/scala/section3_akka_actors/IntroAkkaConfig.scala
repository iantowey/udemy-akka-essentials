package section3_akka_actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App{

  class SimpleActorLogging extends Actor with ActorLogging{
    override def receive: Receive = {
      case message:String => log.info(message)
    }
  }

  /**
   * 1. Inline configuration
   */

  val configString =
    """
      |akka{
      | loglevel = "INFO"
      |}
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system1 = ActorSystem("IntroAkkaConfig_inline_config1", ConfigFactory.load(config))

  val actor1 = system1.actorOf(Props[SimpleActorLogging])
  actor1 ! "hey hey !!"

  /**
   * 2. via config file, default file name is 'application.conf'
   */
  val system2 = ActorSystem("IntroAkkaConfig_inline_config2", ConfigFactory.load())
  val actor2 = system2.actorOf(Props[SimpleActorLogging])
  actor2 ! "hey hey !!"

  /**
   * 3. separate config namespaces in the sample file
   */
  val namespaceConfig = ConfigFactory.load().getConfig("dev")
  val system3 = ActorSystem("IntroAkkaConfig_inline_config3", namespaceConfig)
  val actor3 = system3.actorOf(Props[SimpleActorLogging])
  actor3 ! "hey hey !!"

  /**
   * 4. separate file
   */
  val separateFileConfig = ConfigFactory.load("dev.conf")
  val system4 = ActorSystem("IntroAkkaConfig_inline_config4", separateFileConfig)
  val actor4 = system4.actorOf(Props[SimpleActorLogging])
  actor4 ! "hey hey !!"
  println(s"hocon :: ${separateFileConfig.getString("akka.loglevel")}")

  /**
   * 5. different file formats
   */
  val jsonConfigFile = ConfigFactory.load("application.json")
  println(s"json :: ${jsonConfigFile.getString("akka.loglevel")}")

  val propsConfigFile = ConfigFactory.load("application.properties")
  println(s"property :: ${propsConfigFile.getString("akka.loglevel")}")

}
