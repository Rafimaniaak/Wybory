package com.election.dao;

import com.election.model.ElectionResult;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ElectionResultDAO {

    public ElectionResult findByCandidate(String candidate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM ElectionResult WHERE candidateName = :candidate",
                            ElectionResult.class
                    )
                    .setParameter("candidate", candidate)
                    .uniqueResult();
        }
    }

    public void saveOrUpdate(ElectionResult result) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.merge(result); // Używamy merge zamiast save/update
            transaction.commit();
        }
    }

    // Pozostałe metody
    public void addResult(ElectionResult result) {
        saveOrUpdate(result);
    }

    public List<ElectionResult> getAllResults() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ElectionResult", ElectionResult.class).list();
        }
    }
}