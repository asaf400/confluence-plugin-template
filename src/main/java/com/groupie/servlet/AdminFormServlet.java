package com.groupie.servlet;

import java.net.URI;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import javax.inject.Inject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

public class AdminFormServlet extends HttpServlet{
    @ComponentImport
    private final UserManager userManager;

    @ComponentImport
    private final LoginUriProvider loginUriProvider;

    @ComponentImport
    private final TemplateRenderer templateRenderer;

    @Inject
    public AdminFormServlet(UserManager userManager, LoginUriProvider loginUriProvider, TemplateRenderer templateRenderer) {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.templateRenderer = templateRenderer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        if (!hasAdminPrivileges()) {
            redirectToLogin(req, resp);
            return;
        }

        resp.setContentType("text/html");
        templateRenderer.render("admin.vm", resp.getWriter());
    }

    private boolean hasAdminPrivileges() {
        UserProfile userProfile = userManager.getRemoteUser();
        if (userProfile == null) {
            return false;
        }

        UserKey userKey = userProfile.getUserKey();
        if (!userManager.isSystemAdmin(userKey)) {
            return false;
        }

        return true;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }

    private URI getUri(HttpServletRequest request)
    {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null)
        {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }
}