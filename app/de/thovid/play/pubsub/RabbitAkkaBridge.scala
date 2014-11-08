package de.thovid.play.pubsub

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Success

import com.github.sstone.amqp.Amqp.Ack
import com.github.sstone.amqp.Amqp.AddBinding
import com.github.sstone.amqp.Amqp.Binding
import com.github.sstone.amqp.Amqp.DeclareExchange
import com.github.sstone.amqp.Amqp.Delivery
import com.github.sstone.amqp.Amqp.ExchangeParameters
import com.github.sstone.amqp.Amqp.Publish
import com.github.sstone.amqp.Amqp.QueueParameters
import com.github.sstone.amqp.ChannelOwner
import com.github.sstone.amqp.ChannelOwner.NotConnectedError
import com.github.sstone.amqp.ConnectionOwner
import com.github.sstone.amqp.Consumer
import com.rabbitmq.client.ConnectionFactory

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import play.api.Logger

trait EventParser {
  def apply(data: Array[Byte]): Object
}

trait RabbitAkkaSubscriberConfig {
  def subscriberConnectionFactory: ConnectionFactory
  def subscribeToExchange: ExchangeParameters
  def subscribeToQueue: QueueParameters
}

class RabbitAkkaSubscriberBridge(private val config: RabbitAkkaSubscriberConfig, private val parser: EventParser) {

  def startSubscriber(akkaSystem: akka.actor.ActorSystem) = {
    val conn = akkaSystem.actorOf(ConnectionOwner.props(config.subscriberConnectionFactory, 1 second))
    ConnectionOwner.createChildActor(conn, Consumer.props(listener = Some(listener(akkaSystem)),
      init = List(DeclareExchange(config.subscribeToExchange),
        AddBinding(Binding(config.subscribeToExchange, config.subscribeToQueue, "")))))
  }

  private def listener(system: akka.actor.ActorSystem) = system.actorOf(Props(new Actor {
    def receive = {
      case Delivery(consumerTag, envelope, properties, body) => {
        system.eventStream.publish(parser(body))
        sender ! Ack(envelope.getDeliveryTag)
      }
    }
  }))
}

trait RabbitAkkaPublisherConfig {
  def publisherConnectionFactory: ConnectionFactory
  def publishToExchange: ExchangeParameters
}

trait EventSerializer[T] {
  def eventType: Class[T]
  def apply(event: T): Array[Byte]
  def canSerialize(event: AnyRef): Boolean = eventType.isAssignableFrom(event.getClass())
}

class RabbitAkkaPublisher[T](private val serializer: EventSerializer[T], private val config: RabbitAkkaPublisherConfig) {

  def startPublisher(akkaSystem: akka.actor.ActorSystem)(implicit ec: ExecutionContext) = {
    val conn = akkaSystem.actorOf(ConnectionOwner.props(config.publisherConnectionFactory, 10 seconds))
    val channel = ConnectionOwner.createChildActor(conn, ChannelOwner.props(init = List(DeclareExchange(config.publishToExchange))))

    val subscriber = akkaSystem.actorOf(Props(classOf[RabbitEventPublisher[T]], channel, config.publishToExchange.name, serializer, ec))
    akkaSystem.eventStream.subscribe(subscriber, serializer.eventType)
  }

  private class RabbitEventPublisher[T](channel: ActorRef, exchange: String, serializer: EventSerializer[T], ec: ExecutionContext) extends Actor {
    def receive = {
      case event: AnyRef if (serializer.canSerialize(event)) => sendEvent(event.asInstanceOf[T])(ec)
    }

    private def sendEvent(e: T)(implicit ec: ExecutionContext) = {
      Logger.info(s"sending $e via exchange $exchange...")
      implicit val timeout = Timeout(30.seconds)
      val res = channel ? Publish(exchange, "", serializer(e))
      res.onComplete {
        case Success(response) if (response.isInstanceOf[NotConnectedError]) => {
          context.system.scheduler.scheduleOnce(1.second) {
            self ! e
          }
        }
        case _ => ()
      }
    }
  }
}
