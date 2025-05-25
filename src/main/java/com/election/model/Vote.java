package com.election.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // Mapowanie na kolumnę user_id
    private User user;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false) // Mapowanie na kolumnę candidate_id
    private Candidate candidate;

    @Column(name = "vote_time", nullable = false)
    private LocalDateTime voteTime = LocalDateTime.now();

    // Gettery i Settery
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public LocalDateTime getVoteTime() {
        return voteTime;
    }
}