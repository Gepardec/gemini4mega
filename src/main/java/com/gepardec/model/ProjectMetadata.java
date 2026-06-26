package com.gepardec.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectMetadata {

    private String name;
    private String description;
    private String techStack;
    private List<String> bookingRules = new ArrayList<>();
    private List<String> commonMistakes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProjectMetadata name(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectMetadata description(String description) {
        this.description = description;
        return this;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }

    public ProjectMetadata techStack(String techStack) {
        this.techStack = techStack;
        return this;
    }

    public List<String> getBookingRules() {
        return bookingRules;
    }

    public void setBookingRules(List<String> bookingRules) {
        this.bookingRules = bookingRules == null ? new ArrayList<>() : new ArrayList<>(bookingRules);
    }

    public ProjectMetadata bookingRules(List<String> bookingRules) {
        setBookingRules(bookingRules);
        return this;
    }

    public List<String> getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(List<String> commonMistakes) {
        this.commonMistakes = commonMistakes == null ? new ArrayList<>() : new ArrayList<>(commonMistakes);
    }

    public ProjectMetadata commonMistakes(List<String> commonMistakes) {
        setCommonMistakes(commonMistakes);
        return this;
    }
}
