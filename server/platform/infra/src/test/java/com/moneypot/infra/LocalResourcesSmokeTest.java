package com.moneypot.infra;

import com.moneypot.inbox.InboxListener;
import com.moneypot.infra.adapter.inbox.RabbitInboxAdapterConfiguration;
import com.moneypot.infra.adapter.outbox.RabbitOutboxAdapterConfiguration;
import com.moneypot.infra.client.rabbit.RabbitClientConfiguration;
import com.moneypot.outbox.OutboxMessage;
import com.moneypot.outbox.OutboxPublisher;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real configuration against the RabbitMQ container from {@code bootstrap/local} (port 5673) with nothing but
 * the {@code local} profile. The test is skipped when the container is not running, so the normal build never needs
 * Docker. Start it with {@code bootstrap/local/local.ps1 up}.
 */
class LocalResourcesSmokeTest {

   private static boolean answers(int port) {
      try (Socket socket = new Socket()) {
         socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   static final CopyOnWriteArrayList<String> RECEIVED = new CopyOnWriteArrayList<>();
   static final CountDownLatch DELIVERED = new CountDownLatch(1);
   static final String AGGREGATE = "smoke" + UUID.randomUUID().toString().substring(0, 8);

   @Configuration(proxyBeanMethods = false)
   static class ServiceWithOneInboxUseCase {

      @Bean
      InboxListener inboxListener() {
         return new InboxListener() {
            @Override
            public void listen(String messageId, String signature, String payloadJson) {
               RECEIVED.add(signature + "|" + payloadJson);
               DELIVERED.countDown();
            }

            @Override
            public Set<String> subscribedAggregates() {
               return Set.of(AGGREGATE);
            }
         };
      }
   }

   @Configuration(proxyBeanMethods = false)
   @Import({ServiceWithOneInboxUseCase.class, RabbitClientConfiguration.class,
       RabbitOutboxAdapterConfiguration.class, RabbitInboxAdapterConfiguration.class})
   static class Service {
   }

   @Test
   void anOutboxMessageTravelsThroughRealRabbitMqIntoTheInboxListener() throws Exception {
      Assumptions.assumeTrue(answers(5673),
          "RabbitMQ is not running; start the local resources with bootstrap/local/local.ps1 up");

      try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Service.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .properties(
              "moneypot.outbox.client=rabbit.core",
              "moneypot.inbox.client=rabbit.core",
              "moneypot.adapter.rabbit.consumer.group=smoke-" + UUID.randomUUID())
          .run()) {
         String payload = "{\"aggregate\":\"" + AGGREGATE + "\",\"action\":\"created\",\"data\":{\"id\":\"1\"}}";
         context.getBean(OutboxPublisher.class).publish(new OutboxMessage() {
            public UUID id() { return UUID.randomUUID(); }
            public String aggregateType() { return AGGREGATE; }
            public String aggregateId() { return "1"; }
            public String payload() { return payload; }
            public Instant createdAt() { return Instant.now(); }
         });

         assertThat(DELIVERED.await(60, TimeUnit.SECONDS))
             .as("the inbox listener should receive the message published to RabbitMQ")
             .isTrue();
         assertThat(RECEIVED).containsExactly(AGGREGATE + ".created|{\"id\":\"1\"}");
      }
   }
}
