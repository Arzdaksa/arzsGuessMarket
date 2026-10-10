package server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

// POST /logout
public class LogoutServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(response, () -> {
            HttpSession session = request.getSession(false);
            // AppListener.sessionDestroyed releases the user name
            if (session != null) session.invalidate();
            return null;
        });
    }
}
