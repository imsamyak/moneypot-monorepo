package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.port.in.usecase.CreateDemoPotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.CreateDemoPotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;
import com.moneypot.pot.port.out.persistence.SavePotPort;
import com.moneypot.pot.service.support.DemoSupport;
import com.moneypot.pot.service.support.PotAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

/**
 * Creates a practice pot so a new member can try the whole game alone: Rs 1,000 each, five members (the member and four
 * simulated ones), a 5 minute bidding window, bidding open straight away. Nothing is asked of the wallet.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreateDemoPotService implements CreateDemoPotUseCase {

   private static final long CONTRIBUTION = 100_000L;
   private static final int MIN_BID_PERCENT = 60;
   private static final int BID_WINDOW_SECONDS = 300;

   private final SavePotPort savePotPort;
   private final DemoSupport demoSupport;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotResult execute(CreateDemoPotCmd cmd) {
      Instant now = Instant.now();
      Pot pot = savePotPort.save(Pot.createDemo(cmd.userId(), CONTRIBUTION, MIN_BID_PERCENT, BID_WINDOW_SECONDS, now));
      demoSupport.start(pot, now);
      return potAssembler.assemble(pot, cmd.userId());
   }
}
