package com.example.lobbyservice.repository;

import com.example.lobbyservice.model.LobbyState;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LobbyStateRepository extends CrudRepository<LobbyState , String> {
}
