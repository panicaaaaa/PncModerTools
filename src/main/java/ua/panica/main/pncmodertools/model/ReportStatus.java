package ua.panica.main.pncmodertools.model;

public enum ReportStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED;
    
    public static ReportStatus fromString(String status) {
        for (ReportStatus rs : values()) {
            if (rs.name().equalsIgnoreCase(status)) {
                return rs;
            }
        }
        return PENDING;
    }
}
