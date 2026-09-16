package com.moneypot.pot.domain.exception;

import com.moneypot.shared.exception.notfound.EntityNotFoundException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PotNotFoundException extends EntityNotFoundException {

   private final UUID potId;

   public PotNotFoundException(UUID potId) {
      super("Pot", potId);
      this.potId = potId;
   }

   public static PotNotFoundException of(UUID potId) {
      return new PotNotFoundException(potId);
   }
}
