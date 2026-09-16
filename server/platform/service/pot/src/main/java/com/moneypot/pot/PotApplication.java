package com.moneypot.pot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PotApplication {

   public static void main(String[] args) {
      SpringApplication.run(PotApplication.class, args);
   }
}
