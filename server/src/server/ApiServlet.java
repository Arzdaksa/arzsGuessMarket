package server;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dto.ErrorDto;
import engine.InsufficientFundsException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

// shared plumbing for the JSON endpoints: state lookup, body parsing, and one place that turns
// engine exceptions into HTTP status codes
abstract class ApiServlet extends HttpServlet {

    static final String MARKET_ATTRIBUTE = "guessMarket.service";
    static final String USERNAME_ATTRIBUTE = "guessMarket.username";
    static final Gson GSON = new Gson();

    @FunctionalInterface
    interface Handler {
        Object handle() throws Exception;
    }

    MarketService market() {
        return (MarketService) getServletContext().getAttribute(MARKET_ATTRIBUTE);
    }

    static String username(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(USERNAME_ATTRIBUTE);
    }

    static <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        try (Reader reader = new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8)) {
            T body = GSON.fromJson(reader, type);
            if (body == null) {
                throw new IllegalArgumentException("Request body is required.");
            }
            return body;
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Malformed request body.");
        }
    }

    static int intParam(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a whole number.");
        }
    }

    static long longParam(HttpServletRequest request, String name, long fallback) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a whole number.");
        }
    }

    // runs the handler and writes its result as JSON (or 204 for null); failures become an ErrorDto
    void respond(HttpServletResponse response, Handler handler) throws IOException {
        try {
            Object result = handler.handle();
            if (result == null) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                writeJson(response, HttpServletResponse.SC_OK, result);
            }
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (NoSuchElementException e) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (IllegalStateException | InsufficientFundsException e) {
            writeError(response, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (Exception e) {
            log("Unexpected failure handling request", e);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }

    static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(body));
    }

    static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, new ErrorDto(message == null ? "Request failed." : message));
    }
}
