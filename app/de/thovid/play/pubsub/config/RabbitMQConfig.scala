package de.thovid.play.pubsub.config

import com.github.sstone.amqp.Amqp.QueueParameters
import com.typesafe.config.ConfigFactory
import com.rabbitmq.client.ConnectionFactory
import com.github.sstone.amqp.Amqp.ExchangeParameters
import de.thovid.play.pubsub.RabbitAkkaSubscriberConfig
import de.thovid.play.pubsub.RabbitAkkaPublisherConfig

object RabbitMQConfig extends RabbitAkkaSubscriberConfig with RabbitAkkaPublisherConfig {

  private lazy val publisherHost = ConfigFactory.load().getString("rabbitmq.publisher.host");
  private lazy val publisherExchange = ConfigFactory.load().getString("rabbitmq.publisher.exchange");
  private lazy val subscriberHost = ConfigFactory.load().getString("rabbitmq.subscriber.host");
  private lazy val subscriberQueue = ConfigFactory.load().getString("rabbitmq.subscriber.queue");
  private lazy val subscriberExchange = ConfigFactory.load().getString("rabbitmq.subscriber.exchange");

  lazy val publisherConnectionFactory = {
    val res = new ConnectionFactory()
    res.setUri(publisherHost)
    res
  }

  lazy val subscriberConnectionFactory = {
    val res = new ConnectionFactory()
    res.setUri(subscriberHost)
    res
  }

  lazy val publishToExchange = ExchangeParameters(name = publisherExchange, passive = false, exchangeType = "fanout")

  lazy val subscribeToExchange = ExchangeParameters(name = subscriberExchange, passive = false, exchangeType = "fanout")

  lazy val subscribeToQueue = QueueParameters(subscriberQueue, passive = false, durable = false, exclusive = false, autodelete = false)
}