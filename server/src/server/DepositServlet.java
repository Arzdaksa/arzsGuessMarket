package server;

import dto.DepositRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// POST /account/deposit
public class DepositServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        respond(response, () -> {
            market().deposit(username(request), readJson(request, DepositRequest.class).amount());
            return null;
        });
    }
}
