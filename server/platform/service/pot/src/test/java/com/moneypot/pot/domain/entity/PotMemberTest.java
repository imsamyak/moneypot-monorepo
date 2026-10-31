package com.moneypot.pot.domain.entity;

import com.moneypot.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PotMemberTest {

   private PotMember member() {
      return PotMember.join(UUID.randomUUID(), UUID.randomUUID(), 1);
   }

   @Test
   void aNewMemberIsPendingUntilTheirPaymentArrives() {
      PotMember member = member();

      assertThat(member.isPending()).isTrue();
      member.activate();
      assertThat(member.isActive()).isTrue();
      assertThatThrownBy(member::activate).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void aMemberWhoLeftCanJoinAgainButAMemberWhoIsInCannot() {
      PotMember member = member();
      member.activate();

      assertThatThrownBy(member::rejoin).isInstanceOf(BusinessRuleException.class);
      member.leave();
      assertThat(member.isLeft()).isTrue();
      member.rejoin();
      assertThat(member.isPending()).isTrue();
   }
}
