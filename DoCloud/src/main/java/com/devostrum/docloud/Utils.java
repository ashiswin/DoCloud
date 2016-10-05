package com.devostrum.docloud;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by ashiswin on 31/8/14.
 */
public class Utils {
    public static String hashPassword(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (md != null) {
            md.update(password.getBytes());
            byte[] mb = md.digest();
            String hashedPassword = "";
            for (byte temp : mb) {
                String s = Integer.toHexString(temp);
                while (s.length() < 2) {
                    s = "0" + s;
                }
                s = s.substring(s.length() - 2);
                hashedPassword += s;
            }

            return hashedPassword;
        }

        return null;
    }
}
