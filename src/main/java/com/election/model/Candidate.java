package com.election.model;

import javafx.beans.property.*;
import jakarta.persistence.*;

@Entity
@Table(name = "CANDIDATE")
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String party;

    @Column(nullable = false)
    private int votes;

    // Transient properties dla JavaFX
    private transient StringProperty nameProperty;
    private transient IntegerProperty votesProperty;

    // Konstruktory
    public Candidate() {
        // Domyślny konstruktor dla JPA
    }

    public Candidate(String name, String party, int votes) {
        this.name = name;
        this.party = party;
        this.votes = votes;
        initializeProperties();
    }

    // Inicjalizacja właściwości JavaFX
    private void initializeProperties() {
        this.nameProperty = new SimpleStringProperty(name);
        this.votesProperty = new SimpleIntegerProperty(votes);
    }

    // Gettery i settery dla JPA
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (nameProperty != null) {
            nameProperty.set(name);
        }
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
        if (votesProperty != null) {
            votesProperty.set(votes);
        }
    }

    // Właściwości JavaFX
    public StringProperty nameProperty() {
        if (nameProperty == null) {
            nameProperty = new SimpleStringProperty(name);
        }
        return nameProperty;
    }

    public IntegerProperty votesProperty() {
        if (votesProperty == null) {
            votesProperty = new SimpleIntegerProperty(votes);
        }
        return votesProperty;
    }

    @Override
    public String toString() {
        return name + (party != null ? " (" + party + ")" : "");
    }
}