package com.election.model; // Zmień pakiet na odpowiedni

public class CandidateResult {
    private String candidateName;
    private String party;
    private int votes;

    // Konstruktor
    public CandidateResult(String candidateName, String party, int votes) {
        this.candidateName = candidateName;
        this.party = party;
        this.votes = votes;
    }

    // Gettery
    public String getCandidateName() { return candidateName; }
    public String getParty() { return party; }
    public int getVotes() { return votes; }
}