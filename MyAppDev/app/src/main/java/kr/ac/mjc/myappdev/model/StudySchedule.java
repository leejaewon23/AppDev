package kr.ac.mjc.myappdev.model;

import com.google.firebase.Timestamp;

public class StudySchedule {
    private String scheduleId;
    private String studyPostId;
    private String studyTitle;
    private String title;
    private String description;
    private Timestamp scheduledAt;
    private String createdByUid;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public StudySchedule() {}

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getStudyPostId() {
        return studyPostId;
    }

    public void setStudyPostId(String studyPostId) {
        this.studyPostId = studyPostId;
    }

    public String getStudyTitle() {
        return studyTitle == null ? "" : studyTitle;
    }

    public void setStudyTitle(String studyTitle) {
        this.studyTitle = studyTitle;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Timestamp scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getCreatedByUid() {
        return createdByUid == null ? "" : createdByUid;
    }

    public void setCreatedByUid(String createdByUid) {
        this.createdByUid = createdByUid;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
