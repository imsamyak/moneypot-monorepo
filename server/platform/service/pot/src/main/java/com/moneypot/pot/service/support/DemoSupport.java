package com.moneypot.pot.service.support;

import com.moneypot.pot.domain.entity.Bid;
import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadTransferPort;
import com.moneypot.pot.port.out.persistence.SaveBidPort;
import com.moneypot.pot.port.out.persistence.SaveMemberPort;
import com.moneypot.pot.port.out.persistence.SavePotPort;
import com.moneypot.pot.port.out.persistence.SaveRoundPort;
import com.moneypot.pot.port.out.persistence.SaveTransferPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Plays a demo pot: the simulated members are given random user ids (they have no account), they bid in every round, and
 * everything the pot would ask the wallet to move is treated as already done, so no wallet is involved and no money moves.
 * It follows the same rules as a real pot (the same settlement, the same round order), only without the waiting.
 */
@Component
@RequiredArgsConstructor
public class DemoSupport {

   public static final int SIMULATED_MEMBERS = 4;

   private final SavePotPort savePotPort;
   private final SaveMemberPort saveMemberPort;
   private final LoadMemberPort loadMemberPort;
   private final SaveRoundPort saveRoundPort;
   private final SaveBidPort saveBidPort;
   private final LoadTransferPort loadTransferPort;
   private final SaveTransferPort saveTransferPort;
   private final RoundSupport roundSupport;

   /** Adds the simulated members to a new demo pot, starts it, and opens round 1 with the simulated bids in. */
   public void start(Pot pot, Instant now) {
      saveMemberPort.save(activeMember(pot, pot.getOwnerId(), 1));
      for (int i = 0; i < SIMULATED_MEMBERS; i++) {
         saveMemberPort.save(activeMember(pot, UUID.randomUUID(), i + 2));
      }
      pot.start(SIMULATED_MEMBERS + 1);
      Pot started = savePotPort.save(pot);
      openBidding(started, saveRoundPort.save(Round.open(started.getId(), 1)), group(started), now);
   }

   /**
    * The round was settled, so pay out at once: close the round, mark the collector, and open the next one (or finish the
    * pot). When only one member is left they are paid the whole pot with no bidding, like in a real pot.
    */
   public void payOutAndAdvance(Pot pot, Round round, List<PotMember> group, Instant now) {
      List<RoundTransfer> transfers = loadTransferPort.loadByRoundId(round.getId());
      transfers.stream().filter(RoundTransfer::isRequested).forEach(RoundTransfer::markDone);
      saveTransferPort.saveAll(transfers);

      round.settled();
      saveRoundPort.save(round);
      group.stream().filter(member -> member.getUserId().equals(round.getWinnerId())).findFirst().ifPresent(collector -> {
         collector.markWon(round.getNumber());
         saveMemberPort.save(collector);
      });

      if (group.stream().allMatch(PotMember::isWon)) {
         pot.complete();
         savePotPort.save(pot);
         return;
      }
      pot.nextRound();
      Pot saved = savePotPort.save(pot);
      openBidding(saved, saveRoundPort.save(Round.open(saved.getId(), saved.getCurrentRound())), group, now);
   }

   private void openBidding(Pot pot, Round round, List<PotMember> group, Instant now) {
      if (group.stream().filter(member -> !member.isWon()).count() == 1) {
         roundSupport.settle(pot, round, group);
         payOutAndAdvance(pot, round, group, now);
         return;
      }
      round.startBidding(now.plusSeconds(pot.getBidWindowSeconds()));
      saveRoundPort.save(round);
      placeSimulatedBids(pot, round, group, now);
   }

   /** Every simulated member who has not collected yet bids 80 to 95 percent of the pot, never below the lowest allowed. */
   private void placeSimulatedBids(Pot pot, Round round, List<PotMember> group, Instant now) {
      for (PotMember member : group) {
         if (member.getUserId().equals(pot.getOwnerId()) || member.isWon()) {
            continue;
         }
         long percent = 80 + (member.getJoinOrder() * 7L + round.getNumber() * 5L) % 16;
         long amount = Math.max(pot.minBid(), pot.potAmount() * percent / 100);
         saveBidPort.save(Bid.place(round.getId(), member.getUserId(), amount, now));
      }
   }

   private List<PotMember> group(Pot pot) {
      return loadMemberPort.loadByPotId(pot.getId()).stream().filter(PotMember::isActive).toList();
   }

   private static PotMember activeMember(Pot pot, UUID userId, int order) {
      PotMember member = PotMember.join(pot.getId(), userId, order);
      member.activate();
      return member;
   }
}
