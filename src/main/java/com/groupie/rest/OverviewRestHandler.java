package com.groupie.rest;

import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupie.config.ConfigModels;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.SpaceStatus;
import com.atlassian.confluence.labels.Label;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.lang.Iterable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A resource of message.
 */
@Path("/overview")
@Named
public class OverviewRestHandler {
    @ComponentImport
    private final UserManager userManager;

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    @ComponentImport
    private final LabelManager labelManager;

    @ComponentImport
    private final SpaceManager spaceManager;

    @Inject
    public OverviewRestHandler(UserManager userManager, PluginSettingsFactory pluginSettingsFactory,
                               LabelManager labelManager, SpaceManager spaceManager) {
        this.userManager = userManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.labelManager = labelManager;
        this.spaceManager = spaceManager;
    }

    private List<ConfigModels.LabelConfig> getPluginSettings() {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        Object jsonSettings = pluginSettings.get(ConfigRestHandler.CONFIGURATION_JSON_SETTINGS_KEY);
        List<ConfigModels.LabelConfig> settings = new ArrayList<>();
        if (jsonSettings != null && !((String) jsonSettings).isEmpty()) {
            try {
                settings = new ObjectMapper().readValue((String)jsonSettings, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                System.out.println("Could not parse plugin configuration");
            }
        }

        return settings;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMessage(@Context HttpServletRequest request) {
        if (!hasAdminPrivileges()) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        // Get all spaces
        Collection<String> activeSpaces = spaceManager.getAllSpaceKeys(SpaceStatus.CURRENT);
        Collection<String> archivedSpaces = spaceManager.getAllSpaceKeys(SpaceStatus.ARCHIVED);

        Stream<String> allSpaces = Stream.concat(
                activeSpaces.stream(),
                archivedSpaces.stream());

        // Map spaces to labels
        Map<String, List<Label>> allSpacesToLabels = new HashMap<>();
        for (String space : (Iterable<String>) allSpaces::iterator) {
            List<Label> labels = labelManager.getTeamLabelsForSpace(space);
            if (!labels.isEmpty()) {
                allSpacesToLabels.put(space, labels);
            }
        }

        // Map spaces of both security and standard (non-security) labels
        List<ConfigModels.LabelConfig> pluginSettings = getPluginSettings();
        List<ConfigModels.LabelOverview> securityLabelsOverview = new ArrayList<>();
        List<ConfigModels.LabelOverview> standardLabelsOverview = new ArrayList<>();

        for (String space : allSpacesToLabels.keySet()) {
            List<Label> spaceLabels = allSpacesToLabels.get(space);

            // Filter space labels to security ones
            List<String> securityLabels = pluginSettings.stream()
                    .filter(config -> spaceLabels.stream().anyMatch(label -> label.getName().equals(config.label)))
                    .map(config -> config.label)
                    .collect(Collectors.toList());

            // Filter space labels to standard (non-security) ones
            List<String> standardLabels = spaceLabels.stream()
                    .filter(label -> pluginSettings.stream().noneMatch(config -> label.getName().equals(config.label)))
                    .map(Label::getName)
                    .collect(Collectors.toList());

            // Add the filtered settings to the filteredSpacesAndLabels map
            if (!securityLabels.isEmpty()) {
                securityLabelsOverview.add(new ConfigModels.LabelOverview(space, securityLabels));
            }

            if (!standardLabels.isEmpty()) {
                standardLabelsOverview.add(new ConfigModels.LabelOverview(space, standardLabels));
            }
        }

        Map<String, List<ConfigModels.LabelOverview>> combinedResponseJson = new HashMap<>(){{
            put("securityLabelsOverview", securityLabelsOverview);
            put("standardLabelsOverview", standardLabelsOverview);
        }};

        return Response.ok(combinedResponseJson).build();
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
}