package com.sheepdog.mashmesh.servlets;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.sheepdog.mashmesh.models.UserProfile;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequireProfileFilter implements Filter {
    private static final String PROFILE_PATH = "/view/profile/";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    private UserProfile getUserProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();

        if (user == null) {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
            return null;
        }

        UserProfile userProfile = UserProfile.get(user);

        if (userProfile == null) {
            userProfile = UserProfile.create(user);

            if (!req.getRequestURI().startsWith(PROFILE_PATH)) {
                resp.sendRedirect(PROFILE_PATH);
                return null;
            }
        }

        req.setAttribute("user", user);
        req.setAttribute("userProfile", userProfile);

        return userProfile;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            HttpServletResponse resp = (HttpServletResponse) servletResponse;
            UserProfile userProfile = getUserProfile(req, resp);

            if (userProfile == null) {
                return; // Don't continue chaining if we've performed a redirect
            }
        } catch (ClassCastException e) {
            // Ignore the exception, we're somehow running in an impossible non-HTTP servlet.
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
