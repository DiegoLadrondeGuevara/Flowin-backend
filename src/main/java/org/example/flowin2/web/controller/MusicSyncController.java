package org.example.flowin2.web.controller;

import org.example.flowin2.domain.sala.model.MusicState;
import org.example.flowin2.infrastructure.security.JwtService;
import org.example.flowin2.web.dto.sala.MusicActionDTO;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles music synchronization via WebSocket.
 *
 * Flow:
 * 1. Host sends play/pause/seek/change_track actions
 * 2. Server updates in-memory state and broadcasts to all subscribers
 * 3. New users request current state via REST endpoint
 */
@Controller
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MusicSyncController {

    private final SimpMessagingTemplate messagingTemplate;
    private final JwtService jwtService;

    // In-memory state per sala — sufficient for single-server (Railway)
    private final ConcurrentHashMap<Long, MusicState> musicStates = new ConcurrentHashMap<>();

    public MusicSyncController(SimpMessagingTemplate messagingTemplate, JwtService jwtService) {
        this.messagingTemplate = messagingTemplate;
        this.jwtService = jwtService;
    }

    /**
     * Host sends music actions: play, pause, seek, change_track
     */
    @MessageMapping("/music.action")
    public void handleMusicAction(MusicActionDTO action,
                                  @Header("Authorization") String token) {
        // Validate token
        String cleanToken = token.replace("Bearer ", "");
        jwtService.extractUserName(cleanToken); // throws if invalid

        Long salaId = action.getSalaId();
        MusicState state = musicStates.computeIfAbsent(salaId, id -> new MusicState());
        state.setSalaId(salaId);
        state.setLastUpdateTimestamp(System.currentTimeMillis());

        switch (action.getAction()) {
            case "play":
                state.setPlaying(true);
                state.setCurrentTime(action.getCurrentTime());
                break;
            case "pause":
                state.setPlaying(false);
                state.setCurrentTime(action.getCurrentTime());
                break;
            case "seek":
                state.setCurrentTime(action.getCurrentTime());
                break;
            case "change_track":
                state.setSongName(action.getSongName());
                state.setSongUrl(action.getSongUrl());
                state.setTrackIndex(action.getTrackIndex());
                state.setCurrentTime(0);
                state.setPlaying(true);
                break;
            default:
                return; // Invalid action, ignore
        }

        // Broadcast updated state to all users in the sala
        messagingTemplate.convertAndSend(
                "/topic/sala/" + salaId + "/music",
                state
        );
    }

    /**
     * REST endpoint for new users joining — get current music state.
     * This avoids the need for WebSocket subscription to "catch up".
     */
    @GetMapping("/sala/{salaId}/music-state")
    @ResponseBody
    public MusicState getCurrentMusicState(@PathVariable Long salaId) {
        MusicState state = musicStates.get(salaId);
        if (state == null) {
            // No music playing yet, return empty state
            MusicState empty = new MusicState();
            empty.setSalaId(salaId);
            empty.setPlaying(false);
            return empty;
        }

        // Calculate actual position if playing (drift correction)
        if (state.isPlaying()) {
            long elapsed = System.currentTimeMillis() - state.getLastUpdateTimestamp();
            double adjustedTime = state.getCurrentTime() + (elapsed / 1000.0);
            MusicState adjusted = new MusicState(
                    state.getSalaId(),
                    state.getSongName(),
                    state.getSongUrl(),
                    adjustedTime,
                    state.isPlaying(),
                    System.currentTimeMillis(),
                    state.getTrackIndex()
            );
            return adjusted;
        }

        return state;
    }
}
