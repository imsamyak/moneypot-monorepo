package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.Pot;


public interface SavePotPort {

   Pot save(Pot pot);
}
