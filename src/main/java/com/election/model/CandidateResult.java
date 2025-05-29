package com.election.model;

public class CandidateResult {
    private Long id;          // Dodano pole id
    private String name;      // Zmieniono candidateName → name
    private String party;
    private int votes;

    // Konstruktor z 4 parametrami (id, name, party, votes)
    public CandidateResult(Long id, String name, String party, int votes) {
        this.id = id;
        this.name = name;
        this.party = party;
        this.votes = votes;
    }

    // Gettery
    public Long getId() { return id; }
    public String getName() { return name; } // Zmieniono getCandidateName → getName
    public String getParty() { return party; }
    public int getVotes() { return votes; }
}