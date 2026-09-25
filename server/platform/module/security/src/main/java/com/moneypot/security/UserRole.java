package com.moneypot.security;

public enum UserRole {
   ROLE_MEMBER,
   ROLE_ADMIN;

   /**
    * Parses a role name, with or without the {@code ROLE_} prefix and ignoring case.
    *
    * @return the role, or {@code null} if the text is empty or unknown
    */
   public static UserRole fromString(String roleStr) {
      if (roleStr == null || roleStr.trim().isEmpty()) return null;
      String normalized = roleStr.trim().toUpperCase();
      if (!normalized.startsWith("ROLE_")) {
         normalized = "ROLE_" + normalized;
      }
      for (UserRole role : values()) {
         if (role.name().equalsIgnoreCase(normalized)) {
            return role;
         }
      }
      return null;
   }
}
