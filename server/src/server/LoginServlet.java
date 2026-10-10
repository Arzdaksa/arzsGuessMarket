package server;

import dto.LoginResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// POST /login?username=NAME
public class LoginServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(response, () -> {
            String current = username(request);
            if (current != null) {
                throw new IllegalStateException("This client is already logged in as '" + current + "'.");
            }
            String name = market().login(request.getParameter("username"));
            request.getSession(true).setAttribute(USERNAME_ATTRIBUTE, name);
            return new LoginResponse(name);
        });
    }
}
