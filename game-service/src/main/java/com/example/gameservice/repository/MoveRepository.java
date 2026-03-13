package com.example.gameservice.repository;

import com.example.gameservice.model.Move;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MoveRepository extends MongoRepository<Move,String> {


    List<Move> findByGameId(UUID gameId);

    List<Move> findByPlayerId(UUID playerId);

    @Query(value = "{ 'gameId': ?0 }", sort = "{ 'timestamp': -1 }")
    List<Move> findRecentMovesByGameId(UUID gameId, int limit);

    long countByGameId(UUID gameId);
}
