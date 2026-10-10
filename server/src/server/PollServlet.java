package server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// GET /poll?version=V&chat=C: the full market if it changed since version V, plus chat messages after C
public class PollServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(response, () -> market().poll(longParam(request, "version", 0), longParam(request, "chat", 0)));
    }
}
