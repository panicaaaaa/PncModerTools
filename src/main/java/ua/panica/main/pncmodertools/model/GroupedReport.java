package ua.panica.main.pncmodertools.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupedReport {

    private final UUID targetId;
    private final String targetName;
    private final List<Report> reports;

    public GroupedReport(UUID targetId, String targetName) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.reports = new ArrayList<>();
    }

    public void addReport(Report report) {
        reports.add(report);
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public List<Report> getReports() {
        return reports;
    }

    public int getReportCount() {
        return reports.size();
    }

    public Report getLatestReport() {
        return reports.isEmpty() ? null : reports.get(reports.size() - 1);
    }

    public List<String> getReporters() {
        List<String> reporters = new ArrayList<>();
        for (Report report : reports) {
            if (!reporters.contains(report.getReporterName())) {
                reporters.add(report.getReporterName());
            }
        }
        return reporters;
    }

    public List<String> getReasons() {
        List<String> reasons = new ArrayList<>();
        for (Report report : reports) {
            reasons.add(report.getReason());
        }
        return reasons;
    }

    public boolean hasStatus(ReportStatus status) {
        for (Report report : reports) {
            if (report.getStatus() == status) {
                return true;
            }
        }
        return false;
    }

    public ReportStatus getHighestPriorityStatus() {
        if (hasStatus(ReportStatus.IN_PROGRESS)) {
            return ReportStatus.IN_PROGRESS;
        }
        if (hasStatus(ReportStatus.PENDING)) {
            return ReportStatus.PENDING;
        }
        return ReportStatus.RESOLVED;
    }
}
