package server;

import dto.CloseRequest;
import dto.CreateEventRequest;
import dto.OrderRequest;
import dto.TradeRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.NoSuchElementException;

// POST /events/create, and POST /events/{open|close|trade|order}?id=EVENT_ID
public class EventsServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(response, () -> {
            String user = username(request);
            String action = request.getPathInfo() == null ? "" : request.getPathInfo();
            switch (action) {
                case "/create":
                    return market().createEvent(user, readJson(request, CreateEventRequest.class));
                case "/open":
                    market().openEvent(user, intParam(request, "id"));
                    return null;
                case "/close":
                    market().closeEvent(user, intParam(request, "id"),
                            readJson(request, CloseRequest.class).winningOption());
                    return null;
                case "/trade": {
                    TradeRequest trade = readJson(request, TradeRequest.class);
                    return market().trade(user, intParam(request, "id"), trade.option(), trade.quantity());
                }
                case "/order":
                    return market().submitOrder(user, intParam(request, "id"), readJson(request, OrderRequest.class));
                default:
                    throw new NoSuchElementException("Unknown event action '" + action + "'.");
            }
        });
    }
}
