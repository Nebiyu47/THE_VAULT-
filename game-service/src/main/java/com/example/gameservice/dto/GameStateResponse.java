package com.example.gameservice.dto;

import com.example.gameservice.model.Box;
import com.example.gameservice.model.Game;
import com.example.gameservice.model.GamePlayer;
import com.example.gameservice.model.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameStateResponse {

    private UUID gameId;
    private String roomCode;
    private GameStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int jackpotAmount;
    private int currentJackpot;
    private int totalBoxes;
    private int openedBoxes;
    private List<GamePlayer> players;
    private List<Box> boxes; // Boxes without revealing types for non-architect
    private LocalDateTime greedTimerStarted;
    private int greedTimerSeconds;
    private boolean isGreedTimerActive;
    private int remainingGreedSeconds;
    private UUID currentTurnPlayerId;
    private List<MoveResponse> recentMoves;
    public static GameStateResponse fromGame(Game game, boolean isArchitect) {
        GameStateResponse response = GameStateResponse.builder()
                .gameId(game.getGameId())
                .roomCode(game.getRoomCode())
                .status(game.getStatus())
                .startTime(game.getStartTime())
                .endTime(game.getEndTime())
                .jackpotAmount(game.getJackpotAmount())
                .currentJackpot(game.getCurrentJackpot())
                .totalBoxes(game.getTotalBoxes())
                .openedBoxes(game.getOpenedBoxes())
                .players(game.getPlayers())
                .greedTimerStarted(game.getGreedTimerStarted())
                .greedTimerSeconds(game.getGreedTimerSeconds())
                .isGreedTimerActive(game.isGreedTimerActive())
                .build();
              if(isArchitect){
                  response.setBoxes(game.getBoxes());
              }else {
                  List<Box>filterBoxes = game.getBoxes().stream()
                          .map(box->{
                              if(box.isOpened()){
                                  return box;
                              }else {
                                  Box hiddenBox = new Box();
                                  hiddenBox.setBoxId(box.getBoxId());
                                  hiddenBox.setPosition(box.getPosition());
                                  hiddenBox.setRow(box.getRow());
                                  hiddenBox.setCol(box.getCol());
                                  hiddenBox.setOpened(false);
                                  hiddenBox.setHeatmapValue(box.getHeatmapValue());
                                  return hiddenBox;
                              }
                          })
                          .toList();
                  response.setBoxes(filterBoxes);
              }
              return response;
    }
}
