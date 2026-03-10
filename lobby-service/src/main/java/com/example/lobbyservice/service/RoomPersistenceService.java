package com.example.lobbyservice.service;

import com.example.lobbyservice.model.GameRoom;

import com.example.lobbyservice.repository.mongodb.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomPersistenceService {

 private final RoomRepository roomRepository;
 public void archiveRoom(GameRoom room){
     log.info("Archiving room: {}", room.getRoomCode());
 }
 public void savedGameResults(String roomCode , Object gameResult){
     log.info("Saving game results for room: {}", roomCode);
 }
}
