package com.sheepdog.mashmesh.models;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;

@Entity
public class UserProfile {
    public static enum UserType { NEW, VOLUNTEER, PATIENT };

    @Id private String userId;
    @Index private UserType type = UserType.NEW;
    @Index private String fullName = "";
    @Index private String email = "";
    @Unindex private String address = "";
    @Unindex private GeoPt location = null;
    @Unindex private String comments = "";

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public UserType getType() {
        return type;
    }

    public void setType(UserType type) {
        this.type = type;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public GeoPt getLocation() {
        return location;
    }

    public void setLocation(GeoPt location) {
        this.location = location;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public static UserProfile getOrCreate(User user) {
        Key<UserProfile> userProfileKey = Key.create(UserProfile.class, user.getUserId());
        UserProfile userProfile = OfyService.ofy().load().key(userProfileKey).now();

        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserId(user.getUserId());
            userProfile.setEmail(user.getEmail());
        }

        return userProfile;
    }

    public static UserProfile getByEmail(String email) {
        UserProfile userProfile = OfyService.ofy().load().type(UserProfile.class)
                .filter("email ==", email).first().now();
        return userProfile; // TODO: Raise an exception if userProfile is null.
    }
}
