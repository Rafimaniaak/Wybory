package com.election.model;

import jakarta.persistence.*;

@Entity
@Table(name = "election_results")
public class ElectionResult {
    @Column(name = "candidate_name") // Dopasuj do nazwy kolumny w bazie
    private String candidateName;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int votes;

    // Konstruktor domyĹ›lny (wymagany przez Hibernate)
    public ElectionResult() {}

    // Konstruktor z parametrami
    public ElectionResult(String candidateName, int votes) {
        this.candidateName = candidateName;
        this.votes = votes;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }
}
