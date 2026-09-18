package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.PotMember;


public interface SaveMemberPort {

   PotMember save(PotMember member);
}
