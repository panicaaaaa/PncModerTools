package ua.panica.main.pncmodertools.model;

import java.util.UUID;

public class Report {
    
    private int id;
    private UUID reporterId;
    private String reporterName;
    private UUID targetId;
    private String targetName;
    private String reason;
    private ReportStatus status;
    private UUID handlerId;
    private String handlerName;
    private long createdAt;
    private long handledAt;
    
    public Report(int id, UUID reporterId, String reporterName, UUID targetId, String targetName, 
                  String reason, ReportStatus status, UUID handlerId, String handlerName, 
                  long createdAt, long handledAt) {
        this.id = id;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.reason = reason;
        this.status = status;
        this.handlerId = handlerId;
        this.handlerName = handlerName;
        this.createdAt = createdAt;
        this.handledAt = handledAt;
    }
    
    public int getId() {
        return id;
    }
    
    public UUID getReporterId() {
        return reporterId;
    }
    
    public String getReporterName() {
        return reporterName;
    }
    
    public UUID getTargetId() {
        return targetId;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public String getReason() {
        return reason;
    }
    
    public ReportStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReportStatus status) {
        this.status = status;
    }
    
    public UUID getHandlerId() {
        return handlerId;
    }
    
    public void setHandlerId(UUID handlerId) {
        this.handlerId = handlerId;
    }
    
    public String getHandlerName() {
        return handlerName;
    }
    
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getHandledAt() {
        return handledAt;
    }
    
    public void setHandledAt(long handledAt) {
        this.handledAt = handledAt;
    }
}
