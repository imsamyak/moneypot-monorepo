package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.port.in.usecase.SettleRoundUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.SettleRoundCmd;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Settles every round whose bidding time is over, so the winner is decided without anyone pressing a button. Disable
 * with {@code pot.settlement.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pot.settlement", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RoundSettlementScheduler {

   private static final int BATCH = 50;

   private final LoadRoundPort loadRoundPort;
   private final SettleRoundUseCase settleRoundUseCase;

   @Scheduled(fixedDelayString = "${pot.settlement.interval-ms:5000}")
   public void settleRoundsWhoseBiddingEnded() {
      for (Round round : loadRoundPort.loadBiddingEndedBefore(Instant.now(), BATCH)) {
         try {
            settleRoundUseCase.execute(new SettleRoundCmd(round.getPotId(), false));
            log.info("Settled round {} of pot {}", round.getNumber(), round.getPotId());
         } catch (Exception e) {
            log.error("Could not settle round {} of pot {}; will retry on the next tick", round.getNumber(), round.getPotId(), e);
         }
      }
   }
}
