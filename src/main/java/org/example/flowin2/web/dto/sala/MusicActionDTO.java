package org.example.flowin2.web.dto.sala;

import lombok.Data;

/**
 * DTO for music sync actions sent via WebSocket.
 */
@Data
public class MusicActionDTO {
    private Long salaId;
    private String action;        // "play", "pause", "seek", "change_track"
    private String songName;
    private String songUrl;
    private double currentTime;   // seconds
    private int trackIndex;
}
