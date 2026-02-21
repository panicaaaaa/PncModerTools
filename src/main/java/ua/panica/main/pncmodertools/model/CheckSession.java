package ua.panica.main.pncmodertools.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CheckSession {

    private final Player moderator;
    private final Player suspect;
    private final Location originalLocation;
    private final long startTime;

    public long endsAtMillis;
    public boolean frozen;
    public boolean timerPaused;
    public long graceUntil;

    public CheckSession(Player moderator, Player suspect, Location originalLocation, long durationSeconds) {
        this.moderator = moderator;
        this.suspect = suspect;
        this.originalLocation = originalLocation;
        this.startTime = System.currentTimeMillis();
        this.endsAtMillis = startTime + (durationSeconds * 1000L);
        this.frozen = false;
        this.timerPaused = false;
        this.graceUntil = 0;
    }

    public Player getModerator() {
        return moderator;
    }

    public Player getSuspect() {
        return suspect;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getRemainingMillis() {
        if (timerPaused) {
            return endsAtMillis - startTime;
        }
        return Math.max(0, endsAtMillis - System.currentTimeMillis());
    }

    public boolean isExpired() {
        return !timerPaused && System.currentTimeMillis() >= endsAtMillis;
    }

    public UUID getModeratorId() {
        return moderator.getUniqueId();
    }

    public UUID getSuspectId() {
        return suspect.getUniqueId();
    }
}