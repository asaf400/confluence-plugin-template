package com.groupie.servlet;

import com.atlassian.confluence.labels.Label;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.setup.settings.GlobalSettingsManager;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.groupie.rest.ConfigRestHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junitpioneer.jupiter.*;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

public class AuthorizationServletFilterTest {
    HttpServletRequest request;
    HttpServletResponse response;
    AuthorizationServletFilter servletFilter;
    FilterChain filterChain;
    UserAccessor userAccessor;
    LabelManager labelManager;
    PluginSettings pluginSettings;
    PluginSettingsFactory pluginSettingsFactory;
    GlobalSettingsManager globalSettingsManager;


    @BeforeEach
    public void setup() {
        request = mock(HttpServletRequestWrapper.class);
        response = mock(HttpServletResponse.class);

        userAccessor = mock(UserAccessor.class);
        labelManager = mock(LabelManager.class);
        pluginSettings = mock(PluginSettings.class);
        pluginSettingsFactory = mock(PluginSettingsFactory.class);
        globalSettingsManager = mock(GlobalSettingsManager.class);

        servletFilter = new AuthorizationServletFilter(userAccessor, labelManager, pluginSettingsFactory, globalSettingsManager);
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    @SetEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME, value="1")
    public void testFailsafeEnabledWithStringValue() throws ServletException, IOException {
        servletFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(request);
        verifyNoInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @SetEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME, value="")
    public void testFailsafeEnabledWithEmptyValue() throws ServletException, IOException {
        servletFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(request);
        verifyNoInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testNoSpace() throws ServletException, IOException {
        when(request.getParameterMap()).thenReturn(new HashMap<String, String>());
        servletFilter.doFilter(request, response, filterChain);

        verify(request, atLeastOnce()).getParameterMap();
        verifyNoInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testMissingSettingsKey() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(null);

        servletFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testInvalidSettingsJson() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn("invalid-json");

        servletFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testInvalidSettingsJsonSchema() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(
                "{\"secret-label\": [\"space\", \"another-space\"]}"
        );

        servletFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testNoSpaceLabels() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(
                "[{\"label\": \"secret\", \"allowedGroups\": [\"users\", \"admins\"]}, {\"label\": \"top-secret\", \"allowedGroups\": [\"admins\"]}]"
        );

        when(labelManager.getTeamLabelsForSpace("space")).thenReturn(new ArrayList<>());

        servletFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testNoSpaceSecurityLabels() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(
                "[{\"label\": \"secret\", \"allowedGroups\": [\"users\", \"admins\"]}, {\"label\": \"top-secret\", \"allowedGroups\": [\"admins\"]}]"
        );

        when(labelManager.getTeamLabelsForSpace("space")).thenReturn(List.of(new Label("non-security-label")));

        servletFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testPermittedSingleSpaceSecurityLabel() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(
                "[{\"label\": \"secret\", \"allowedGroups\": [\"users\", \"admins\"]}, {\"label\": \"top-secret\", \"allowedGroups\": [\"admins\"]}]"
        );

        when(labelManager.getTeamLabelsForSpace("space")).thenReturn(List.of(new Label("secret")));
        when(userAccessor.getGroupNamesForUserName("username")).thenReturn(List.of("users"));

        try (MockedStatic<AuthenticatedUserThreadLocal> authenticatedUserThreadLocal = Mockito.mockStatic(AuthenticatedUserThreadLocal.class)) {
            authenticatedUserThreadLocal.when(AuthenticatedUserThreadLocal::getUsername).thenReturn("username");

            servletFilter.doFilter(request, response, filterChain);

            verifyNoInteractions(response);
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testPermittedMultipleSpaceSecurityLabels() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(
                "[{\"label\": \"secret\", \"allowedGroups\": [\"users\", \"admins\"]}, {\"label\": \"top-secret\", \"allowedGroups\": [\"admins\"]}]"
        );

        when(labelManager.getTeamLabelsForSpace("space")).thenReturn(List.of(new Label("secret", "top-secret")));
        when(userAccessor.getGroupNamesForUserName("username")).thenReturn(List.of("users", "admins"));

        try (MockedStatic<AuthenticatedUserThreadLocal> authenticatedUserThreadLocal = Mockito.mockStatic(AuthenticatedUserThreadLocal.class)) {
            authenticatedUserThreadLocal.when(AuthenticatedUserThreadLocal::getUsername).thenReturn("username");

            servletFilter.doFilter(request, response, filterChain);

            verifyNoInteractions(response);
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testNotPermitted() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(
                "[{\"label\": \"secret\", \"allowedGroups\": [\"users\", \"admins\"]}, {\"label\": \"top-secret\", \"allowedGroups\": [\"admins\"]}]"
        );

        when(labelManager.getTeamLabelsForSpace("space")).thenReturn(List.of(new Label("secret")));
        when(userAccessor.getGroupNamesForUserName("username")).thenReturn(List.of("non-relevant-group"));

        Settings settings = new Settings();
        settings.setBaseUrl("base-url");
        when(globalSettingsManager.getGlobalSettings()).thenReturn(settings);

        try (MockedStatic<AuthenticatedUserThreadLocal> authenticatedUserThreadLocal = Mockito.mockStatic(AuthenticatedUserThreadLocal.class)) {
            authenticatedUserThreadLocal.when(AuthenticatedUserThreadLocal::getUsername).thenReturn("username");

            servletFilter.doFilter(request, response, filterChain);

            verifyNoInteractions(filterChain);
            verify(response, times(1)).reset();
            verify(response, times(1)).setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            verify(response, times(1)).setHeader("Location", settings.getBaseUrl() + AuthorizationServletFilter.BLOCKED_REQUEST_REDIRECT_PATH);
        }
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testPartiallyPermitted() throws ServletException, IOException {
        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
        when(request.getParameterMap()).thenReturn(parameterMap);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(
                "[{\"label\": \"secret\", \"allowedGroups\": [\"users\", \"admins\"]}, {\"label\": \"top-secret\", \"allowedGroups\": [\"admins\"]}]"
        );

        when(labelManager.getTeamLabelsForSpace("space")).thenReturn(List.of(new Label("secret"), new Label("top-secret")));
        when(userAccessor.getGroupNamesForUserName("username")).thenReturn(List.of("non-relevant-group", "users"));

        Settings settings = new Settings();
        settings.setBaseUrl("base-url");
        when(globalSettingsManager.getGlobalSettings()).thenReturn(settings);

        try (MockedStatic<AuthenticatedUserThreadLocal> authenticatedUserThreadLocal = Mockito.mockStatic(AuthenticatedUserThreadLocal.class)) {
            authenticatedUserThreadLocal.when(AuthenticatedUserThreadLocal::getUsername).thenReturn("username");

            servletFilter.doFilter(request, response, filterChain);

            verifyNoInteractions(filterChain);
            verify(response, times(1)).reset();
            verify(response, times(1)).setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            verify(response, times(1)).setHeader("Location", settings.getBaseUrl() + AuthorizationServletFilter.BLOCKED_REQUEST_REDIRECT_PATH);
        }
    }
}
