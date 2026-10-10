package server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

// creates the in-memory market when the app starts, and frees a user's name when their session ends
// (explicit logout, or timeout after the client stops polling)
public class AppListener implements ServletContextListener, HttpSessionListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute(ApiServlet.MARKET_ATTRIBUTE, new MarketService());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(ApiServlet.MARKET_ATTRIBUTE);
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        String username = (String) event.getSession().getAttribute(ApiServlet.USERNAME_ATTRIBUTE);
        MarketService market = (MarketService) event.getSession().getServletContext()
                .getAttribute(ApiServlet.MARKET_ATTRIBUTE);
        if (username != null && market != null) {
            market.logout(username);
        }
    }
}
