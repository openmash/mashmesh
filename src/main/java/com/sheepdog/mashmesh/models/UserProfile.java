package com.sheepdog.mashmesh.models;

import com.google.appengine.api.datastore.GeoPt;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;

import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class UserProfile {
    public static enum UserType { NEW, VOLUNTEER, PATIENT };

    @Id private String userId;
    @Indexed private UserType type = UserType.NEW;
    @Indexed private String fullName = "";
    @Indexed private String email = "";
    @Unindexed private String address = "";
    @Unindexed private GeoPt location = null;
    @Unindexed private String comments = "";

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

    public Key<UserProfile> getKey() {
        return Key.create(UserProfile.class, getUserId());
    }

    public static UserProfile get(User user) {
        Key<UserProfile> userProfileKey = Key.create(UserProfile.class, user.getUserId());
        return OfyService.ofy().find(userProfileKey);
    }

    public static UserProfile create(User user) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(user.getUserId());
        userProfile.setEmail(user.getEmail());
        return userProfile;
    }

    public static UserProfile getOrCreate(User user) {
        UserProfile userProfile = get(user);

        if (userProfile == null) {
            userProfile = create(user);
        }

        return userProfile;
    }

    public static UserProfile getByEmail(String email) {
        UserProfile userProfile = OfyService.ofy()
                .query(UserProfile.class)
                .filter("email", email)
                .get();
        return userProfile; // TODO: Raise an exception if userProfile is null.
    }

    public static QueryResultIterable<UserProfile> listAll() {
        return OfyService.ofy().query(UserProfile.class).fetch();
    }
}
