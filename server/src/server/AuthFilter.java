package server;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// every endpoint except /login requires a logged-in session
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if ("/login".equals(request.getServletPath()) || ApiServlet.username(request) != null) {
            chain.doFilter(req, res);
        } else {
            ApiServlet.writeError((HttpServletResponse) res, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
        }
    }

    @Override
    public void destroy() {
    }
}
