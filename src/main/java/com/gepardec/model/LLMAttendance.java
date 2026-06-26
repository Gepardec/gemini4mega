package com.gepardec.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LLMAttendance {

    private Integer id;
    private OffsetDateTime date;
    private String from;
    private String to;
    private BigDecimal duration;
    private String employeeId;
    private String project;
    private String projectDescription;
    private String projectTask;
    private String note;
    private Boolean billable;
    private String workLocationId;
    private String activityId;
    private Integer subtaskId;
    private Boolean workLocationIsProjectRelevant;
    private String start;
    private String destination;
    private String directionOfTravel;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LLMAttendance id(Integer id) {
        this.id = id;
        return this;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public LLMAttendance date(OffsetDateTime date) {
        this.date = date;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public LLMAttendance from(String from) {
        this.from = from;
        return this;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public LLMAttendance to(String to) {
        this.to = to;
        return this;
    }

    public BigDecimal getDuration() {
        return duration;
    }

    public void setDuration(BigDecimal duration) {
        this.duration = duration;
    }

    public LLMAttendance duration(BigDecimal duration) {
        this.duration = duration;
        return this;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LLMAttendance employeeId(String employeeId) {
        this.employeeId = employeeId;
        return this;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public LLMAttendance project(String project) {
        this.project = project;
        return this;
    }

    public String getProjectTask() {
        return projectTask;
    }

    public void setProjectTask(String projectTask) {
        this.projectTask = projectTask;
    }

    public LLMAttendance projectTask(String projectTask) {
        this.projectTask = projectTask;
        return this;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LLMAttendance note(String note) {
        this.note = note;
        return this;
    }

    public Boolean getBillable() {
        return billable;
    }

    public void setBillable(Boolean billable) {
        this.billable = billable;
    }

    public LLMAttendance billable(Boolean billable) {
        this.billable = billable;
        return this;
    }

    public String getWorkLocationId() {
        return workLocationId;
    }

    public void setWorkLocationId(String workLocationId) {
        this.workLocationId = workLocationId;
    }

    public LLMAttendance workLocationId(String workLocationId) {
        this.workLocationId = workLocationId;
        return this;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public LLMAttendance activityId(String activityId) {
        this.activityId = activityId;
        return this;
    }

    public Integer getSubtaskId() {
        return subtaskId;
    }

    public void setSubtaskId(Integer subtaskId) {
        this.subtaskId = subtaskId;
    }

    public LLMAttendance subtaskId(Integer subtaskId) {
        this.subtaskId = subtaskId;
        return this;
    }

    public Boolean getWorkLocationIsProjectRelevant() {
        return workLocationIsProjectRelevant;
    }

    public void setWorkLocationIsProjectRelevant(Boolean workLocationIsProjectRelevant) {
        this.workLocationIsProjectRelevant = workLocationIsProjectRelevant;
    }

    public LLMAttendance workLocationIsProjectRelevant(Boolean workLocationIsProjectRelevant) {
        this.workLocationIsProjectRelevant = workLocationIsProjectRelevant;
        return this;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public LLMAttendance start(String start) {
        this.start = start;
        return this;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LLMAttendance destination(String destination) {
        this.destination = destination;
        return this;
    }

    public String getDirectionOfTravel() {
        return directionOfTravel;
    }

    public void setDirectionOfTravel(String directionOfTravel) {
        this.directionOfTravel = directionOfTravel;
    }

    public LLMAttendance directionOfTravel(String directionOfTravel) {
        this.directionOfTravel = directionOfTravel;
        return this;
    }
}
