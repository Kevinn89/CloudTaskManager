package com.tex.cloud_task_manager.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

  @Bean
  public DirectExchange userNotificationExchange(
      @Value("${app.rabbitmq.user-notification-exchange}") String exchangeName) {
    return new DirectExchange(exchangeName, true, false);
  }

  @Bean
  public Queue userRegisteredQueue(
      @Value("${app.rabbitmq.user-registered-queue}") String queueName) {
    return new Queue(queueName, true);
  }

  @Bean
  public Binding userRegisteredBinding(
      Queue userRegisteredQueue,
      DirectExchange userNotificationExchange,
      @Value("${app.rabbitmq.user-registered-routing-key}") String routingKey) {
    return BindingBuilder.bind(userRegisteredQueue).to(userNotificationExchange).with(routingKey);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new JacksonJsonMessageConverter();
  }
}
