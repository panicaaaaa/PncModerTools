package ua.panica.main.pncmodertools.model;

import java.util.UUID;

public class StaffWorkSession {
    
    private UUID playerId;
    private String playerName;
    private long startTime;
    private long totalTime;
    private boolean active;
    
    public StaffWorkSession(UUID playerId, String playerName, long startTime, long totalTime, boolean active) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.startTime = startTime;
        this.totalTime = totalTime;
        this.active = active;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getTotalTime() {
        return totalTime;
    }
    
    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }
    
    public void addTime(long time) {
        this.totalTime += time;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public long getCurrentSessionTime() {
        if (!active) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }
    
    public long getTotalTimeWithCurrent() {
        return totalTime + getCurrentSessionTime();
    }
}
