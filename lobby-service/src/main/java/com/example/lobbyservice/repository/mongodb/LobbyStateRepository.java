package com.example.lobbyservice.repository.mongodb;
import com.example.lobbyservice.model.LobbyState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LobbyStateRepository extends MongoRepository<LobbyState , String> {
    Optional<LobbyState> findByRoomCode(String roomCode);

    void deleteByRoomCode(String roomCode);
}
