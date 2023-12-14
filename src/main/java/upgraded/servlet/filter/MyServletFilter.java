package upgraded.servlet.filter;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.*;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import upgraded.config.ConfigResource.SpaceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;


import com.sun.source.util.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import com.atlassian.plugin.spring.scanner.annotation.imports.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.atlassian.confluence.user.AuthenticatedUserThreadLocal.getUsername;

@Named
public class MyServletFilter implements Filter {
    public static final String SPACE_KEY = "spaceKey";
    private static final Logger log = LoggerFactory.getLogger(MyServletFilter.class);
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @ConfluenceImport
    private final UserAccessor userAccessor;

    @Inject
    public MyServletFilter(UserAccessor userAccessor, PluginSettingsFactory pluginSettingsFactory) {
        this.userAccessor = userAccessor;
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    private void redirect(ServletResponse response) {
        response.reset();
        log.info("Blocking request");
        if (response instanceof HttpServletResponse) {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            // TODO: add a message for the user
            resp.setHeader("Location", "http://localhost:1990/confluence/");
        }
    }

    private String getSpaceKey(HttpServletRequestWrapper request) {
        if (!request.getParameterMap().containsKey(SPACE_KEY)) {
            // This happens quite often, we need a way to tell if this is a content request, otherwise we have to block it
            return null;
        }
        return request.getParameter(SPACE_KEY);
    }

    /**
     * We fetch the list of the user's groups, as seen by Confluence.
     * <p>
     * We assume that Confluence fetches the groups using LDAP, and expect
     * the mapping values above to be such groups.
     * Note - we do not query LDAP ourselves. We rely on the fact only admins
     * can s
     *
     * @param username
     * @return
     */
    private List<String> getGroups(String username) {
        return userAccessor.getGroupNamesForUserName(username);
    }

    private PluginSettings initilizeSettings() {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        Object myString = pluginSettings.get("JSON_SETTINGS");
        if (myString == null || ((String) myString).equals("")) {
            pluginSettings.put("JSON_SETTINGS", "[]");
        }
        return pluginSettings;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (System.getenv("DISABLE_SECURITY_PLUGIN") != null) {
            chain.doFilter(request, response);
            return;
        }

        PluginSettings pluginSettings = initilizeSettings();
        String spacesFilter = (String) pluginSettings.get("JSON_SETTINGS");
        ObjectMapper objectMapper = new ObjectMapper();

        List<SpaceConfig> spaceConfigList = objectMapper.readValue(spacesFilter, new TypeReference<List<SpaceConfig>>() {
        });

        Map<String, List<String>> spaceToGroups = new HashMap<String, List<String>>();
        {
            spaceToGroups.put("asdf", Arrays.asList("group1", "confluence-users"));
            spaceToGroups.put("aaa", Arrays.asList("group1", "confluence-users"));
        }

        for (SpaceConfig o : spaceConfigList) {
            spaceToGroups.put(o.spaceName, o.allowedGroups);
        }

        System.out.println("Examining request...");
        if (!(request instanceof HttpServletRequestWrapper)) {
            // TODO understand which requests get here
            redirect(response);
            return;
        }

        // Extract information from the request URL
        HttpServletRequestWrapper wrapped_request = (HttpServletRequestWrapper) request;
        String username = getUsername();
        List<String> confluenceGroups = getGroups(username);

        String space = getSpaceKey(wrapped_request);
        space = space == null ? null : space.toLowerCase();
        if (space == null) {
            chain.doFilter(request, response);
            return;
        }

        List<String> spaceGroups = spaceToGroups.get(space);
        if (spaceGroups == null || spaceGroups.isEmpty()) {
            redirect(response);
            return;
        }

        Set<String> intersection = confluenceGroups.stream()
                .distinct()
                .filter(spaceGroups::contains)
                .collect(Collectors.toSet());

        boolean allowed = !intersection.isEmpty();
        if (!allowed) {
            redirect(response);
            return;
        }

        //continue the request
        chain.doFilter(request, response);
    }


    /* public void foo(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        String requestURI = wrapped_request.getRequestURI();
        String contextPath = wrapped_request.getContextPath();
        // javax.servlet.ServletContext context = wrapped_request.getSession().getServletContext();
        ConfluenceUser confluenceUser = AuthenticatedUserThreadLocal.get();

    }*/
}