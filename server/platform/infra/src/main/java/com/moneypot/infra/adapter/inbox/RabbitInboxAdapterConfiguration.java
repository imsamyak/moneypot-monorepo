package com.moneypot.infra.adapter.inbox;

import com.moneypot.inbox.InboxListener;
import com.moneypot.infra.adapter.ClientReference;
import com.moneypot.infra.client.rabbit.RabbitClients;
import com.moneypot.shared.json.JsonMappers;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Feeds the inbox from the RabbitMQ client the service named with {@code moneypot.inbox.client: rabbit.<name>}. Exchanges
 * follow the outbox adapter's layout ({@code <prefix><aggregate>}). A consumer bean defined by the service replaces
 * this one.
 */
@AutoConfiguration(afterName = {
    "com.moneypot.inbox.internal.config.InboxAutoConfiguration",
    "com.moneypot.infra.client.rabbit.RabbitClientConfiguration"})
@ConditionalOnClass({InboxListener.class, SimpleMessageListenerContainer.class})
@ConditionalOnExpression("'${moneypot.inbox.client:}'.toLowerCase().startsWith('rabbit.')")
public class RabbitInboxAdapterConfiguration {

   @Bean
   @ConditionalOnBean(InboxListener.class)
   @ConditionalOnMissingBean
   RabbitInboxConsumer rabbitInboxConsumer(
       InboxListener listener,
       RabbitClients clients,
       Environment environment,
       @Value("${moneypot.adapter.rabbit.exchange-prefix:moneypot.}") String exchangePrefix,
       @Value("${moneypot.adapter.rabbit.consumer.group:${spring.application.name:moneypot-inbox}}") String group,
       @Value("${moneypot.adapter.rabbit.consumer.max-attempts:8}") int maxAttempts,
       @Value("${moneypot.adapter.rabbit.consumer.backoff-ms:200}") long backoffMs,
       @Value("${moneypot.adapter.rabbit.consumer.prefetch:10}") int prefetch) {
      String client = ClientReference.of(environment, "inbox").name();
      clients.requireDefined(client);
      return new RabbitInboxConsumer(listener, () -> clients.connectionFactory(client), () -> clients.admin(client),
          () -> clients.template(client), JsonMappers.messaging(), exchangePrefix, group, maxAttempts, backoffMs,
          prefetch);
   }
}
