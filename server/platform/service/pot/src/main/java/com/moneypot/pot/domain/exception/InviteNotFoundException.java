package com.moneypot.pot.domain.exception;

import com.moneypot.shared.exception.notfound.EntityNotFoundException;

/** No pot has this invite code (it may have been mistyped). */
public class InviteNotFoundException extends EntityNotFoundException {

   public InviteNotFoundException() {
      super("Invitation", "that code");
   }

   public static InviteNotFoundException of() {
      return new InviteNotFoundException();
   }
}
