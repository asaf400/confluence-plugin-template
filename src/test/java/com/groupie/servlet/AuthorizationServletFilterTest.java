package com.groupie.servlet;

import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.setup.settings.GlobalSettingsManager;
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

import org.junitpioneer.jupiter.*;
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

        verifyZeroInteractions(request);
        verifyZeroInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @SetEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME, value="")
    public void testFailsafeEnabledWithEmptyValue() throws ServletException, IOException {
        servletFilter.doFilter(request, response, filterChain);

        verifyZeroInteractions(request);
        verifyZeroInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
    public void testNoSpace() throws ServletException, IOException {
        when(request.getParameterMap()).thenReturn(new HashMap<String, String>());
        servletFilter.doFilter(request, response, filterChain);

        verify(request, atLeastOnce()).getParameterMap();
        verifyZeroInteractions(response);
        verify(filterChain, times(1)).doFilter(request, response);
    }

//    @Test
//    @ClearEnvironmentVariable(key=AuthorizationServletFilter.FAILSAFE_ENV_VAR_NAME)
//    public void testMissingSettingsKey() throws ServletException, IOException {
//        HashMap<String, String> parameterMap = new HashMap<>();
//        parameterMap.put(AuthorizationServletFilter.REQUEST_PARAMETER_SPACE_KEY, "space");
//        when(request.getParameterMap()).thenReturn(parameterMap);
//
//        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
//        when(pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY)).thenReturn(null);
//
//        servletFilter.doFilter(request, response, filterChain);
//
//        verifyZeroInteractions(response);
//        verify(filterChain, times(1)).doFilter(request, response);
//    }
}
