package com.example.lobbyservice.repository;

import com.example.lobbyservice.model.LobbyState;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LobbyStateRepository extends CrudRepository<LobbyState , String> {
    Optional<LobbyState> findByRoomCode(String roomCode);

    void deleteByRoomCode(String roomCode);
}
