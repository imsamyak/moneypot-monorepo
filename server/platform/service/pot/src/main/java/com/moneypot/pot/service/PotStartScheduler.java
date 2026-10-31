package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.port.in.usecase.StartPotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.StartPotCmd;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Starts every pot whose bidding start time has come: joining closes and round 1 goes to bidding. Disable with
 * {@code pot.start.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pot.start", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PotStartScheduler {

   private static final int BATCH = 50;

   private final LoadPotPort loadPotPort;
   private final StartPotUseCase startPotUseCase;

   @Scheduled(fixedDelayString = "${pot.start.interval-ms:5000}")
   public void startPotsWhoseTimeHasCome() {
      for (Pot pot : loadPotPort.loadOpenStartingBefore(Instant.now(), BATCH)) {
         try {
            startPotUseCase.execute(new StartPotCmd(pot.getId()));
            log.info("Started pot {}", pot.getId());
         } catch (Exception e) {
            log.error("Could not start pot {}; will retry on the next tick", pot.getId(), e);
         }
      }
   }
}
