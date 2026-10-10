package server;

import dto.ChatRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// POST /chat: messages reach the other clients through their next /poll
public class ChatServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(response, () -> {
            market().sendChat(username(request), readJson(request, ChatRequest.class).text());
            return null;
        });
    }
}
