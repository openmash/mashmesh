package com.sheepdog.mashmesh.integration;

public class PatientConfig {
    private String name;
    private String email;
    private String address;
    private String comments = "";

    public String getName() {
        return name;
    }

    public PatientConfig setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public PatientConfig setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public PatientConfig setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getComments() {
        return comments;
    }

    public PatientConfig setComments(String comments) {
        this.comments = comments;
        return this;
    }
}
