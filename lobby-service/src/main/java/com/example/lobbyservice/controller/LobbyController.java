package com.example.lobbyservice.controller;

import com.example.lobbyservice.dto.CreateRoomRequest;
import com.example.lobbyservice.dto.JoinRoomRequest;
import com.example.lobbyservice.dto.PlayerResponse;
import com.example.lobbyservice.dto.RoomResponse;
import com.example.lobbyservice.model.GameConfig;
import com.example.lobbyservice.service.LobbyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lobby")
@RequiredArgsConstructor
@Slf4j
public class LobbyController {

   private final LobbyService lobbyService;
   @PostMapping("/create")
    public ResponseEntity<RoomResponse> createRoom (@Valid @RequestBody CreateRoomRequest request){
       log.info("POST /api/lobby/create - Creating room: {}", request.getRoomName());
       RoomResponse response = lobbyService.createRoom(request);
       return ResponseEntity.status(HttpStatus.CREATED).body(response);
   }
   @PostMapping("/join/{roomCode}")
    public ResponseEntity<RoomResponse>joinRoom(
           @PathVariable String roomCode,
           @Valid @RequestBody JoinRoomRequest request
           ){
       request.setRoomCode(roomCode);
       log.info("POST /api/lobby/join/{} - Player {} joining", roomCode, request.getUsername());
       RoomResponse response = lobbyService.joinRoom(request);
       return ResponseEntity.ok(response);
   }
   @GetMapping("/{roomCode}")
    public ResponseEntity<RoomResponse>getRoom(@PathVariable String roomCode){
       log.info("GET /api/lobby/{} - Getting room info", roomCode);
       RoomResponse response = lobbyService.getRoom(roomCode);
       return ResponseEntity.ok(response);
   }
   @GetMapping("/{roomCode}/players")
    public ResponseEntity<List<PlayerResponse>>getRoomPlayers(@PathVariable String roomCode){
       log.info("GET /api/lobby/{}/players - Getting players", roomCode);
       List<PlayerResponse>player = lobbyService.getRoomPlayers(roomCode);
       return ResponseEntity.ok(player);
   }
   @DeleteMapping("/{roomCode}/leave/{playerId}")
    public ResponseEntity<Void>setPlayerReady(
           @PathVariable String roomCode,
           @PathVariable UUID playerId,
           @RequestParam boolean ready
           ){
       log.info("POST /api/lobby/{}/ready/{} - Setting ready status: {}", roomCode, playerId, ready);
       lobbyService.setPlayerReady(roomCode,playerId,ready);
       return ResponseEntity.ok().build();
   }
   @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>>searchRooms(@RequestParam String query){
       log.info("GET /api/lobby/search - Searching rooms with query: {}", query);
       List<RoomResponse> rooms = lobbyService.searchRooms(query);
       return ResponseEntity.ok(rooms);
   }
   @PostMapping("/{roomCode}/config")
    public ResponseEntity<Void>updateGameConfig(
           @PathVariable String roomCode,
           @RequestBody GameConfig config,
           @RequestParam UUID architectId
           ){
       lobbyService.updateGameConfig(roomCode,config,architectId);
       return ResponseEntity.ok().build();
   }
   @DeleteMapping("/{roomCode}")
    public ResponseEntity<Void>deleteRoom(@PathVariable String roomCode){
       log.info("DELETE /api/lobby/{} - Deleting room", roomCode);
       lobbyService.deleteRoom(roomCode);
       return ResponseEntity.ok().build();
   }

}
