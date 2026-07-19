package com.tex.cloud_task_manager.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationNotificationPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Value("${app.rabbitmq.user-notification-exchange}")
  private String exchange;

  @Value("${app.rabbitmq.user-registered-routing-key}")
  private String routingKey;

  public void publish(UserRegisteredMessage message) {
    try {
      rabbitTemplate.convertAndSend(exchange, routingKey, message);
      log.info("Published user registration notification for userId={}", message.userId());
    } catch (AmqpException ex) {
      log.warn(
          "Unable to publish user registration notification for userId={}", message.userId(), ex);
    }
  }
}
