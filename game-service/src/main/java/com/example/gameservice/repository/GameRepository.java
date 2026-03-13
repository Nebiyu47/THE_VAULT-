package com.example.gameservice.repository;

import com.example.gameservice.model.Game;
import com.example.gameservice.model.enums.GameStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameRepository extends MongoRepository<Game,String> {

   Optional<Game>findByGameId(UUID gameId);
   List<Game>findByStatus(GameStatus status);
   List<Game>findByRoomCode(String roomCode);
   @Query("{'players.playerId': ?0}")
    List<Game> findByPlayerId(UUID playerId);
   @Query("{'status': ?0, 'startTime': {$lt:  ?1 }}")
    List<Game>findStaleGame(GameStatus status , LocalDateTime cutOff);
   void deleteByGameId(UUID gameId);
}
