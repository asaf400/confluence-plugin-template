package com.groupie.rest;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Context;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.plugin.spring.scanner.annotation.imports.*;


import java.io.IOException;
import java.util.List;

import com.groupie.config.ConfigModels;

/**
 * A resource of message.
 */
@Path("/")
@Named
public class ConfigRestHandler {
    public static final String CONFIGURATION_JSON_SETTINGS_KEY = "JSON_SETTINGS";

    @ComponentImport
    private final UserManager userManager;

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    @Inject
    public ConfigRestHandler(UserManager userManager, PluginSettingsFactory pluginSettingsFactory) {
        this.userManager = userManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    private PluginSettings getRawPluginSettings() {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();

        Object jsonSettings = pluginSettings.get(CONFIGURATION_JSON_SETTINGS_KEY);
        if (jsonSettings == null || ((String)jsonSettings).isEmpty()) {
            pluginSettings.put(CONFIGURATION_JSON_SETTINGS_KEY, "");
        }

        return pluginSettings;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putMessage(List<ConfigModels.LabelConfig> content, @Context HttpServletRequest request) throws IOException {
        if (!hasAdminPrivileges()) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        for (ConfigModels.LabelConfig labelConfig : content) {
            if (!isLabelConfigValid(labelConfig)) {
                return Response.status(Status.BAD_REQUEST).build();
            }
        }

        PluginSettings pluginSettings = getRawPluginSettings();
        String jsonString = new ObjectMapper().writeValueAsString(content);

        pluginSettings.put(CONFIGURATION_JSON_SETTINGS_KEY, jsonString);
        return Response.ok().build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMessage(@Context HttpServletRequest request) {
        if (!hasAdminPrivileges()) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        PluginSettings pluginSettings = getRawPluginSettings();
        return Response.ok(pluginSettings.get(CONFIGURATION_JSON_SETTINGS_KEY)).build();
    }

    private boolean hasAdminPrivileges() {
        UserProfile userProfile = userManager.getRemoteUser();
        if (userProfile == null){
            return false;
        }

        UserKey userKey = userProfile.getUserKey();
        if (!userManager.isSystemAdmin(userKey)) {
            return false;
        }

        return true;
    }

    private boolean isLabelConfigValid(ConfigModels.LabelConfig labelConfig) {
        if (labelConfig.label == null || labelConfig.allowedGroups == null) {
            return false;
        }

        return true;
    }
}