package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Bid;
import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.PlaceBidUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.PlaceBidCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;
import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.persistence.LoadBidPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.port.out.persistence.SaveBidPort;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import com.moneypot.shared.exception.security.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;

/**
 * Places or lowers a bid while the bidding is open. Only members who have not won can bid, and a bid has to be
 * between the lowest allowed (a share of the pot the owner chose) and the whole pot.
 */
@Service
@Validated
@RequiredArgsConstructor
public class PlaceBidService implements PlaceBidUseCase {

   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final LoadMemberPort loadMemberPort;
   private final LoadRoundPort loadRoundPort;
   private final LoadBidPort loadBidPort;
   private final SaveBidPort saveBidPort;
   private final PotAssembler potAssembler;
   private final NotifyPotChangedPort notifyPotChangedPort;

   @Override
   @Transactional
   public PotResult execute(PlaceBidCmd cmd) {
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      Instant now = Instant.now();
      Round round = loadRoundPort.loadByPotIdAndNumber(pot.getId(), pot.getCurrentRound())
            .filter(r -> pot.isActive() && r.biddingIsOpen(now))
            .orElseThrow(() -> BusinessRuleException.of("Bidding is not open"));

      PotMember member = loadMemberPort.loadByPotId(pot.getId()).stream()
            .filter(m -> m.getUserId().equals(cmd.userId()))
            .findFirst()
            .orElseThrow(() -> ForbiddenException.of("Only members of the pot can bid"));
      if (member.isWon()) {
         throw BusinessRuleException.of("You have already won a round, so you cannot bid again");
      }
      if (cmd.amount() < pot.minBid()) {
         throw BusinessRuleException.of("The lowest bid allowed is " + pot.minBid() + " paise (" + pot.getMinBidPercent() + "% of the pot)");
      }
      if (cmd.amount() > pot.potAmount()) {
         throw BusinessRuleException.of("A bid cannot be more than the pot, which is " + pot.potAmount() + " paise");
      }

      Bid existing = loadBidPort.loadByRoundIdAndUserId(round.getId(), cmd.userId()).orElse(null);
      if (existing == null) {
         saveBidPort.save(Bid.place(round.getId(), cmd.userId(), cmd.amount(), now));
      } else {
         existing.lowerTo(cmd.amount(), now);
         saveBidPort.save(existing);
      }
      notifyPotChangedPort.potChanged(pot.getId());
      return potAssembler.assemble(pot, cmd.userId());
   }
}
