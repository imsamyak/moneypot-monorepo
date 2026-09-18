package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.PotMember;
import java.util.List;
import java.util.UUID;


public interface LoadMemberPort {

   /** The members of a pot, in join order. */
   List<PotMember> loadByPotId(UUID potId);
}
