package com.visiboard.pc.model;

public class Report {
    private String reportId;
    private String reporterId;
    private String reportedUserId;
    private String reportedNoteId;
    private String reason;
    private long timestamp;
    private String status; // PENDING, ACTION_TAKEN, DISMISSED

    public Report() {}

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }

    public String getReportedNoteId() { return reportedNoteId; }
    public void setReportedNoteId(String reportedNoteId) { this.reportedNoteId = reportedNoteId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    private String reporterName;
    private String reportedName;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReportedName() { return reportedName; }
    public void setReportedName(String reportedName) { this.reportedName = reportedName; }
    
    private String targetDetails;
    private String type; // NOTE or USER

    public String getTargetDetails() { return targetDetails; }
    public void setTargetDetails(String targetDetails) { this.targetDetails = targetDetails; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
