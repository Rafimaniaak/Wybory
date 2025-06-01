package com.election.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "VOTES")
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    @Column(name = "has_voted", nullable = false)
    private boolean hasVoted;

    @Column(name = "vote_time", nullable = false)
    private LocalDateTime voteTime;

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

    public void setVoteTime(LocalDateTime voteTime) {
        this.voteTime = voteTime;
    }
    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }
}