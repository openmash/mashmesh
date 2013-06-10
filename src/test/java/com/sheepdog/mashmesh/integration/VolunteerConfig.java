package com.sheepdog.mashmesh.integration;

import com.sheepdog.mashmesh.models.AvailableTimePeriod;

import java.util.List;

public class VolunteerConfig {
    private String name;
    private String email;
    private int maximumDistance;
    private List<AvailableTimePeriod> availableTimePeriods;
    private String address;
    private String comments = "";

    public String getName() {
        return name;
    }

    public VolunteerConfig setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public VolunteerConfig setEmail(String email) {
        this.email = email;
        return this;
    }

    public int getMaximumDistance() {
        return maximumDistance;
    }

    public VolunteerConfig setMaximumDistance(int maximumDistance) {
        this.maximumDistance = maximumDistance;
        return this;
    }

    public List<AvailableTimePeriod> getAvailableTimePeriods() {
        return availableTimePeriods;
    }

    public VolunteerConfig setAvailableTimePeriods(List<AvailableTimePeriod> availableTimePeriods) {
        this.availableTimePeriods = availableTimePeriods;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public VolunteerConfig setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getComments() {
        return comments;
    }

    public VolunteerConfig setComments(String comments) {
        this.comments = comments;
        return this;
    }
}
