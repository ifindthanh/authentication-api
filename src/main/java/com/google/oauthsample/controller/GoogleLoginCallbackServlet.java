package com.google.oauthsample.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper;
import com.google.oauthsample.Constants;

public class GoogleLoginCallbackServlet extends HttpServlet {
    private static final Collection<String> SCOPES = Arrays.asList("email", "profile");
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/plus/v1/people/me/openIdConnect";
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private GoogleAuthorizationCodeFlow flow;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        // Ensure that this is no request forgery going on, and that the user
        // sending us this connect request is the user that was supposed to.
        if (req.getSession().getAttribute("state") == null
                || !req.getParameter("state").equals((String) req.getSession().getAttribute("state"))) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.sendRedirect("books");
            return;
        }

        req.getSession().removeAttribute("state"); // Remove one-time use state.

        flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                Constants.CLIENT_ID, Constants.CLIENT_SECRET, SCOPES).build();

        String code = req.getParameter("code");
        TokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri("http://localhost:8080/google-authentication/oauth2callback").execute();

        req.getSession().setAttribute("token", tokenResponse.toString()); // Keep track of the token.
        Credential credential = flow.createAndStoreCredential(tokenResponse, null);
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(credential);

        GenericUrl url = new GenericUrl(USERINFO_ENDPOINT); // Make an authenticated request.
        HttpRequest request = requestFactory.buildGetRequest(url);
        request.getHeaders().setContentType("application/json");

        String jsonIdentity = request.execute().parseAsString();
        @SuppressWarnings("unchecked")
        Map<String, String> userIdResult = new ObjectMapper().readValue(jsonIdentity, HashMap.class);
        // From this map, extract the relevant profile info and store it in the session.
        req.getSession().setAttribute("userEmail", userIdResult.get("email"));
        req.getSession().setAttribute("userId", userIdResult.get("sub"));
        req.getSession().setAttribute("userImageUrl", userIdResult.get("picture"));
        resp.sendRedirect((String) req.getSession().getAttribute("loginDestination"));
    }
}
