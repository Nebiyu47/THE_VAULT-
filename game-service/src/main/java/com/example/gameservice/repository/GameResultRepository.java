package com.example.gameservice.repository;

import com.example.gameservice.model.GameResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameResultRepository extends MongoRepository<GameResult , String> {


    Optional<GameResult> findByGameId(UUID gameId);

    List<GameResult> findByWinnerId(UUID winnerId);

    List<GameResult> findByWinnerIdOrderByCompletedAtDesc(UUID winnerId, int limit);
}
