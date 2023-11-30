package upgraded.servlet.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MyServletContextListener implements ServletContextListener
{
    private static final Logger log = LoggerFactory.getLogger(MyServletContextListener.class);

    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        servletContextEvent.getServletContext().setAttribute("myservletcontextlistener", "original");
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent)
    {
        servletContextEvent.getServletContext().setAttribute("myservletcontextlistener", "destroyed");
    }
}
