package com.election.service;

import com.election.exception.ServiceException;
import com.election.model.Candidate;
import com.election.model.User;
import com.election.model.Vote;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;

public class VotingService {

    public void castVote(User user, Candidate candidate) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            if (user == null || candidate == null) {
                throw new IllegalStateException("Nie znaleziono użytkownika lub kandydata.");
            }

            // Sprawdź w bazie czy użytkownik już głosował
            User managedUser = session.get(User.class, user.getId());
            if (managedUser == null) {
                throw new IllegalStateException("Użytkownik nie istnieje w bazie danych!");
            }

            if (managedUser.isHasVoted()) {
                throw new IllegalStateException("Użytkownik już głosował.");
            }

            // Zwiększ liczbę głosów
            Candidate managedCandidate = session.get(Candidate.class, candidate.getId());
            if (managedCandidate == null) {
                throw new IllegalStateException("Kandydat nie istnieje w bazie danych!");
            }

            managedCandidate.setVotes(managedCandidate.getVotes() + 1);
            session.merge(managedCandidate);

            // Utwórz nowy głos
            Vote vote = new Vote();
            vote.setCandidate(managedCandidate);
            vote.setUser(managedUser);
            vote.setVoteTime(LocalDateTime.now());
            session.persist(vote);

            // Zaktualizuj użytkownika
            managedUser.setHasVoted(true);
            session.merge(managedUser);

            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new ServiceException("Błąd podczas głosowania: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}