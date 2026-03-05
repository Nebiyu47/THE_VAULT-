package com.example.lobbyservice.repository;

import com.example.lobbyservice.enums.RoomStatus;
import com.example.lobbyservice.model.GameRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends MongoRepository<GameRoom, String> {

    Optional<GameRoom> findByRoomCode(String roomCode);

    List<GameRoom> findByStatus(RoomStatus status);

    List<GameRoom> findByArchitectId(UUID architectId);

    @Query("{ 'status': ?0, 'createdAt': { '$lt': ?1 } }")
    List<GameRoom> findStaleRooms(RoomStatus status, LocalDateTime cutoff);

    @Query("{ 'players.playerId': ?0 }")
    List<GameRoom> findRoomsByPlayerId(UUID playerId);

    @Query(value = "{ 'roomCode': ?0 }", fields = "{ 'players': 1 }")
    Optional<GameRoom> findPlayersByRoomCode(String roomCode);

    @Query("{ 'roomCode': ?0 }")
    @Update("{ '$push': { 'players': ?1 } }")
    void addPlayerToRoom(String roomCode, Object player);

    @Query("{ 'roomCode': ?0 }")
    @Update("{ '$pull': { 'players': { 'playerId': ?1 } } }")
    void removePlayerFromRoom(String roomCode, UUID playerId);

    @Query("{ 'roomCode': ?0, 'players.playerId': ?1 }")
    @Update("{ '$set': { 'players.$.isReady': ?2 } }")
    void updatePlayerReadyStatus(String roomCode, UUID playerId, boolean isReady);

    void deleteByRoomCode(String roomCode);
}