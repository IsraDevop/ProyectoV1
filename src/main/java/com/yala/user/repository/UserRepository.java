package com.yala.user.repository;

import com.yala.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);

    Optional<User> findByDni(String dni);

    @Query("SELECT COUNT(b) FROM Bid b WHERE b.bidder.id = :userId")
    long countAuctionsParticipatedByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM Auction a WHERE a.winner.id = :userId")
    long countAuctionsWonByUser(@Param("userId") Long userId);
}
