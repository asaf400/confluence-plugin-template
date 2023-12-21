package upgraded.servlet.filter;

import com.atlassian.confluence.labels.Label;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.setup.settings.GlobalSettingsManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ConfluenceImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import upgraded.config.ConfigResource.LabelConfig;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.atlassian.confluence.user.AuthenticatedUserThreadLocal.getUsername;

@Named
public class MyServletFilter implements Filter {
    public static final String REQUEST_PARAMETER_SPACE_KEY = "spaceKey";
    public static final String CONFIGURATION_JSON_SETTINGS_KEY = "JSON_SETTINGS";
    public static final String BLOCKED_REQUEST_REDIRECT_PATH = "";
    public static final String FAILSAFE_ENV_VAR_NAME = "DISABLE_SECURITY_PLUGIN";
    private static final Logger log = LoggerFactory.getLogger(MyServletFilter.class);

    @ConfluenceImport
    private final UserAccessor userAccessor;

    @ConfluenceImport
    private final LabelManager labelManager;

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    @ComponentImport
    private final GlobalSettingsManager globalSettingsManager;

    @Inject
    public MyServletFilter(UserAccessor userAccessor, LabelManager labelManager,
                           PluginSettingsFactory pluginSettingsFactory, GlobalSettingsManager globalSettingsManager) {
        this.userAccessor = userAccessor;
        this.labelManager = labelManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.globalSettingsManager = globalSettingsManager;
    }

    public void init(FilterConfig filterConfig) {
    }

    public void destroy() {
    }

    private void blockRequest(ServletResponse response) {
        log.info("Blocking request");

        response.reset();
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        String baseUrl = globalSettingsManager.getGlobalSettings().getBaseUrl();
        httpServletResponse.setHeader("Location", baseUrl + BLOCKED_REQUEST_REDIRECT_PATH);
    }

    private String getCurrentSpace(HttpServletRequestWrapper request) {
        if (!request.getParameterMap().containsKey(REQUEST_PARAMETER_SPACE_KEY)) {
            return null;
        }
        return request.getParameter(REQUEST_PARAMETER_SPACE_KEY);
    }

    /**
     * We fetch the list of groups in which the user is a member of, as seen by Confluence.
     * We assume that Confluence fetches the groups using LDAP, and expect
     * the configuration of permitted groups to be of such groups.
     * Note: We do not query LDAP ourselves and rely on the fact that only admins create groups and that
     * the groups seen by Confluence are therefore trustworthy.
     */
    private List<String> getUserGroups() {
        String username = getUsername();
        return userAccessor.getGroupNamesForUserName(username);
    }

    private List<LabelConfig> getPluginSettings() {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        Object jsonSettings = pluginSettings.get(CONFIGURATION_JSON_SETTINGS_KEY);
        List<LabelConfig> settings = new ArrayList<>();
        if (jsonSettings != null && !((String) jsonSettings).isEmpty()) {
            try {
                settings = new ObjectMapper().readValue((String)jsonSettings, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.error("Could not parse plugin configuration");
            }
        }

        return settings;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Due to the sensitivity of the plugin and its potential to lead to unavailability, including of admin users,
        // the FAILSAFE_ENV_VAR_NAME environment variable is used as a failsafe mechanism that if set permits
        // all requests
        if (System.getenv(FAILSAFE_ENV_VAR_NAME) != null) {
            chain.doFilter(request, response);
            return;
        }

        // Permit non-space request
        String space = getCurrentSpace((HttpServletRequestWrapper) request);
        space = space == null ? null : space.toLowerCase();
        if (space == null) {
            chain.doFilter(request, response);
            return;
        }

        // Load plugin configuration
        List<LabelConfig> pluginSettings = getPluginSettings();
        Map<String, List<String>> labelsToPermittedGroups = new HashMap<>();
        for (LabelConfig labelConfig : pluginSettings) {
            labelsToPermittedGroups.put(labelConfig.label, labelConfig.allowedGroups);
        }

        // Filter label-level configuration to relevant labels
        List<Label> spaceLabels = labelManager.getTeamLabelsForSpace(space);
        Collection<String> spaceLabelNames = spaceLabels.stream().map(Label::getName).collect(Collectors.toList());
        labelsToPermittedGroups.keySet().retainAll(spaceLabelNames);

        // Ensure membership of the user in at least one group of each of the relevant space labels
        List<String> userGroups = getUserGroups();
        for (String label : labelsToPermittedGroups.keySet()) {
            Set<String> intersection = userGroups.stream()
                    .distinct()
                    .filter(labelsToPermittedGroups.get(label)::contains)
                    .collect(Collectors.toSet());

            if (intersection.isEmpty()) {
                blockRequest(response);
                return;
            }
        }

        // Continue the request
        chain.doFilter(request, response);
    }
}