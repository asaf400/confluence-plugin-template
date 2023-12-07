package upgraded.servlet.filter;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.*;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.SpacePermissionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.plugin.spring.scanner.annotation.imports.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.atlassian.confluence.user.AuthenticatedUserThreadLocal.getUsername;

@Named
public class MyServletFilter implements Filter {

    Map<String, List<String>> spaceToGroups = new HashMap<String, List<String>>();
    {
        spaceToGroups.put("asdf", Arrays.asList("group1", "confluence-users"));
        spaceToGroups.put("aaa", Arrays.asList("group1", "confluence-users"));

    }

    private static final Logger log = LoggerFactory.getLogger(MyServletFilter.class);
    public static final String SPACE_KEY = "spaceKey";
    @ConfluenceImport
    private final PageManager pageManager;

    @ConfluenceImport
    private final SpaceManager spaceManager;

    @ConfluenceImport
    private final UserAccessor userAccessor;

//    @ConfluenceImport
    // Doesn't work
//    private final GroupResolver groupResolver;

    @ConfluenceImport
    private final LabelManager labelManager;

    @ConfluenceImport
    private final PermissionManager permissionManager;

    @ConfluenceImport
    private final SpacePermissionManager spacePermissionManager;

//    @ComponentImport
//    final ActiveObjects activeObjects;

//    @ConfluenceImport
//    private final ConfluenceAuthenticator confluenceAuthenticator;
//    @ConfluenceImport
//    private final ConfluenceUser confluenceUser;
//
//    @ConfluenceImport
//    private final ConfluenceUserManager confluenceUserManager;

    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("FOOBAR2");
    }

    @Inject
    public MyServletFilter(PageManager pageManager, SpaceManager spaceManager, UserAccessor userAccessor /*,GroupResolver groupResolver*/, LabelManager labelManager, PermissionManager permissionManager, SpacePermissionManager spacePermissionManager) {
//        ConfluenceAuthenticator confluenceAuthenticator, ConfluenceUser confluenceUser, ConfluenceUserManager confluenceUserManager
        this.pageManager = pageManager;
        this.spaceManager = spaceManager;
        this.userAccessor = userAccessor;
//        this.groupResolver = groupResolver;
        this.labelManager = labelManager;
        this.permissionManager = permissionManager;
        this.spacePermissionManager = spacePermissionManager;
//        this.confluenceAuthenticator = confluenceAuthenticator;
//        this.authenticatedUserThreadLocal = authenticatedUserThreadLocal;
//        this.confluenceUser = confluenceUser;
//        this.confluenceUserManager = confluenceUserManager;
    }
    public void destroy() {
    }

    public static Page findPageByTitle(List<Page> pages, String titleToFind) {
        for (Page page : pages) {
            if (page.getTitle().equals(titleToFind)) {
                return page;
            }
        }
        return null;  // Not found
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

    private List<String> getLDAPGroups(String username) {
        return userAccessor.getGroupNamesForUserName(username);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.debug("Examining request...");
        if (!(request instanceof HttpServletRequestWrapper)) {
            redirect(response);
            return;
        }

        // Extract information from the request URL
        HttpServletRequestWrapper wrapped_request = (HttpServletRequestWrapper) request;
        String username = getUsername();
        List<String> LDAPGroups = getLDAPGroups(username);

        String space = getSpaceKey(wrapped_request);
        space = space == null ? null : space.toLowerCase();
        List<String> spaceGroups = spaceToGroups.get(space);
        if (spaceGroups == null || spaceGroups.isEmpty()) {
            redirect(response);
            return;
        }

        Set<String> intersection = LDAPGroups.stream()
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

        Space currentSpace;
        List<Page> pages = new ArrayList<>();
        if (wrapped_request.getParameterMap().containsKey(SPACE_KEY) &&
                wrapped_request.getParameter(SPACE_KEY) != null &&
                !wrapped_request.getParameter(SPACE_KEY).trim().isEmpty()) {
            currentSpace = this.spaceManager.getSpace(wrapped_request.getParameter(SPACE_KEY));
            pages = pageManager.getPages(currentSpace,true);
        }

        // Check if the request is for a Confluence page - Conf 8.X
        Page currentPage;
//        Optional<Page> currentPage;
        if (wrapped_request.getParameterMap().containsKey(TITLE) &&
                wrapped_request.getParameter(TITLE) != null &&
                !wrapped_request.getParameter(TITLE).trim().isEmpty()) {

//            currentPage = pages.stream().filter(page -> page.getTitle().equals(wrapped_request.getParameter("title"))).findFirst();
            currentPage = findPageByTitle(pages, wrapped_request.getParameter("title"));
        }

    }*/
}