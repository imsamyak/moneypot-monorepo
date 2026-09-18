package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.Round;


public interface SaveRoundPort {

   Round save(Round round);
}
