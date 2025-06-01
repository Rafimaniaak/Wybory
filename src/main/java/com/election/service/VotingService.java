package com.election.service;

import com.election.model.Candidate;
import com.election.model.ElectionResult;
import com.election.model.User;
import com.election.model.Vote;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;

public class VotingService {

    public void castVote(User user, Candidate candidate) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();

        try {
            if (user == null || candidate == null) {
                throw new IllegalStateException("Nie znaleziono użytkownika lub kandydata.");
            }

            if (user.isHasVoted()) {
                throw new IllegalStateException("Użytkownik już głosował.");
            }

            // Zwiększ liczbę głosów
            candidate.setVotes(candidate.getVotes() + 1);
            session.merge(candidate);

            // Utwórz nowy głos
            Vote vote = new Vote();
            vote.setCandidate(candidate);
            vote.setUser(user);
            vote.setVoteTime(LocalDateTime.now());
            session.save(vote);

            // Zaktualizuj użytkownika
            user.setHasVoted(true);
            session.merge(user);

            // Opcjonalnie: aktualizacja wyników
            updateElectionResults(session, candidate);

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }


    private void updateElectionResults(Session session, Candidate candidate) {
        ElectionResult result = session.createQuery(
                        "FROM ElectionResult WHERE candidateName = :name",
                        ElectionResult.class)
                .setParameter("name", candidate.getName())
                .uniqueResult();

        if (result == null) {
            result = new ElectionResult(candidate.getName(), 1);
        } else {
            result.setVotes(result.getVotes() + 1);
        }

        session.merge(result);
    }

    public boolean hasUserVoted(User user) {
        return user.isHasVoted();
    }
}