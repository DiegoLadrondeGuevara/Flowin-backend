package org.example.flowin2.domain.sala.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * In-memory model representing the current music playback state of a Sala.
 * Not a JPA entity — stored in ConcurrentHashMap in MusicSyncController.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicState {
    private Long salaId;
    private String songName;
    private String songUrl;
    private double currentTime;       // seconds into the track
    private boolean playing;
    private long lastUpdateTimestamp;  // server epoch millis for drift correction
    private int trackIndex;           // index in the sala's playlist
}
