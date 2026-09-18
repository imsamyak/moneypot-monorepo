package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.Bid;


public interface SaveBidPort {

   Bid save(Bid bid);
}
