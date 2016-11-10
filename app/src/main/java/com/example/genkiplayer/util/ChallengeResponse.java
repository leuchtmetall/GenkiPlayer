package com.example.genkiplayer.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ChallengeResponse {
    private String challenge;
    private String response;


    public ChallengeResponse(String passwordPre, String passwordPost) {
        SecureRandom random = new SecureRandom();
        this.challenge = new BigInteger(130, random).toString(32);
        try {
            this.response = Utils.sha1(passwordPre + this.challenge + passwordPost);
        } catch (Exception e) {
            this.response = "";
            e.printStackTrace();
        }
    }

    public String getChallenge() {
        return challenge;
    }

    public boolean checkResponse(String candidate) {
        return response.equals(candidate);
    }
}
