package upgraded.servlet.filter;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.SpacePermissionManager;
import com.atlassian.confluence.user.ConfluenceAuthenticator;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.user.ConfluenceUserManager;
import com.atlassian.crowd.model.authentication.UserAuthenticationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.plugin.spring.scanner.annotation.imports.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
//import com.atlassian.plugin.osgi.bridge.external;

@Named
public class MyServletFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(MyServletFilter.class);
    @ConfluenceImport
    private final PageManager pageManager;

    @ConfluenceImport
    private final SpaceManager spaceManager;

    @ConfluenceImport
    private final UserAccessor userAccessor;

    @ConfluenceImport
    private final LabelManager labelManager;

    @ConfluenceImport
    private final PermissionManager permissionManager;

    @ConfluenceImport
    private final SpacePermissionManager spacePermissionManager;

    @ComponentImport
    final ActiveObjects activeObjects;

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
    public MyServletFilter(PageManager pageManager, SpaceManager spaceManager, UserAccessor userAccessor, LabelManager labelManager, PermissionManager permissionManager, SpacePermissionManager spacePermissionManager, ActiveObjects activeObjects) {
//        ConfluenceAuthenticator confluenceAuthenticator, ConfluenceUser confluenceUser, ConfluenceUserManager confluenceUserManager
        this.pageManager = pageManager;
        this.spaceManager = spaceManager;
        this.userAccessor = userAccessor;
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

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        //do some custom handling here
        System.out.println("FOOBAR4");
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.reset();
        resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        resp.setHeader("Location", "http://localhost:1990/confluence/");

        // Extract information from the request URL
        HttpServletRequestWrapper wrapped_request = (HttpServletRequestWrapper) request;

        String requestURI = wrapped_request.getRequestURI();
        String contextPath = wrapped_request.getContextPath();
        javax.servlet.ServletContext context = wrapped_request.getSession().getServletContext();
        ConfluenceUser confluenceUser = AuthenticatedUserThreadLocal.get();


        Space currentSpace;
        List<Page> pages = new ArrayList<Page>();
        if (wrapped_request.getParameterMap().containsKey("spaceKey") &&
                wrapped_request.getParameter("spaceKey") != null &&
                !wrapped_request.getParameter("spaceKey").trim().isEmpty()) {
            currentSpace = this.spaceManager.getSpace(wrapped_request.getParameter("spaceKey"));
            pages = pageManager.getPages(currentSpace,true);
        }

        // Check if the request is for a Confluence page - Conf 8.X
        Page currentPage;
//        Optional<Page> currentPage;
        if (wrapped_request.getParameterMap().containsKey("title") &&
                wrapped_request.getParameter("title") != null &&
                !wrapped_request.getParameter("title").trim().isEmpty()) {

//            currentPage = pages.stream().filter(page -> page.getTitle().equals(wrapped_request.getParameter("title"))).findFirst();
            currentPage = findPageByTitle(pages, wrapped_request.getParameter("title"));
        }

        // Check if the request is for a Confluence page - Conf 6.X
        if (requestURI.startsWith(contextPath + "/pages/")) {
            // Extract space key and page title from the request URL
            String pageIdParam = wrapped_request.getParameter("pageId");

            if (pageIdParam != null) {
                long pageId = Long.parseLong(pageIdParam);

                // Retrieve Page and Space objects using ComponentAccessor
//                PageManager pageManager = ComponentAccessor.getComponent(PageManager.class);
//                SpaceManager spaceManager = ComponentAccessor.getComponent(SpaceManager.class);

                // Retrieve Page object using injected PageManager
//                Page page = pageManager.getPage(pageId);

//                if (page != null) {
//                    System.out.println("Request is for a Confluence page:");
//                    System.out.println("Page Title: " + page.getTitle());
//                    System.out.println("Space Key: " + page.getSpaceKey());
//                }
            } else {
                System.out.println("Either PageManager is not available or missing pageId. Unable to retrieve detailed page information.");
            }
        }
        //continue the request
        chain.doFilter(request, response);
    }
}