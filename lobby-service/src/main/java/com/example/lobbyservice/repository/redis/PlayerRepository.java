package com.example.lobbyservice.repository.redis;

import com.example.lobbyservice.model.LobbyPlayer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface PlayerRepository extends CrudRepository<LobbyPlayer, UUID> {
}
