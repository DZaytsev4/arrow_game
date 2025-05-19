package com.example.gamearrow.model;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PlayerDAO {
    public Player findOrCreatePlayer(String name) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            Player player = session.createQuery("FROM Player WHERE name = :name", Player.class)
                    .setParameter("name", name)
                    .uniqueResult();

            if (player == null) {
                player = new Player(name);
                session.persist(player);
                session.flush();
            }

            tx.commit();
            return player;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public void incrementVictories(String playerName) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            Player player = session.createQuery("FROM Player WHERE name = :name", Player.class)
                    .setParameter("name", playerName)
                    .uniqueResult();

            if (player != null) {
                player.incrementVictories();
                session.merge(player);
                session.flush();
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }
    public List<Player> getAllPlayers() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createQuery("FROM Player ORDER BY victories DESC", Player.class)
                    .getResultList();
        } finally {
            session.close();
        }
    }
}