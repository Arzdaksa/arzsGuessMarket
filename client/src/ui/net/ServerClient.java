package ui.net;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dto.ChatRequest;
import dto.CloseRequest;
import dto.CreateEventRequest;
import dto.DepositRequest;
import dto.ErrorDto;
import dto.EventDto;
import dto.LoginResponse;
import dto.OrderRequest;
import dto.OrderResultDto;
import dto.PollResponse;
import dto.Side;
import dto.TradeDto;
import dto.TradeRequest;
import dto.UploadResultDto;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

// the client's only route to the market: every call is a blocking HTTP request to the server,
// so callers must stay off the JavaFX thread. The session cookie from login is kept per instance.
public final class ServerClient {

    public static final String DEFAULT_URL = "http://localhost:8080/guess-market";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .cookieHandler(new CookieManager())
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final Gson gson = new Gson();
    private final String baseUrl;

    public ServerClient(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("Server address must start with http:// or https://");
        }
        URI.create(trimmed);
        this.baseUrl = trimmed;
    }

    public String baseUrl() {return baseUrl;}

    public String login(String username) {
        String query = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
        return post("/login?" + query, BodyPublishers.noBody(), null, LoginResponse.class).username();
    }

    public void logout() {
        post("/logout", BodyPublishers.noBody(), null, Void.class);
    }

    public PollResponse poll(long knownVersion, long chatCursor) {
        return send(request("/poll?version=" + knownVersion + "&chat=" + chatCursor).GET(), PollResponse.class);
    }

    public UploadResultDto uploadXml(Path file) {
        try {
            return post("/upload", BodyPublishers.ofFile(file), "application/xml", UploadResultDto.class);
        } catch (IOException e) {
            throw new ServerException(0, "Cannot read " + file + ": " + e.getMessage());
        }
    }

    public EventDto createEvent(CreateEventRequest request) {
        return postJson("/events/create", request, EventDto.class);
    }

    public void openEvent(int eventId) {
        post("/events/open?id=" + eventId, BodyPublishers.noBody(), null, Void.class);
    }

    public void closeEvent(int eventId, String winningOption) {
        postJson("/events/close?id=" + eventId, new CloseRequest(winningOption), Void.class);
    }

    public TradeDto trade(int eventId, String option, int quantity) {
        return postJson("/events/trade?id=" + eventId, new TradeRequest(option, quantity), TradeDto.class);
    }

    public OrderResultDto submitOrder(int eventId, String option, Side side, int quantity, double price) {
        return postJson("/events/order?id=" + eventId, new OrderRequest(option, side, quantity, price),
                OrderResultDto.class);
    }

    public void deposit(double amount) {
        postJson("/account/deposit", new DepositRequest(amount), Void.class);
    }

    public void sendChat(String text) {
        postJson("/chat", new ChatRequest(text), Void.class);
    }

    private <T> T postJson(String path, Object body, Class<T> responseType) {
        return post(path, BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8),
                "application/json; charset=UTF-8", responseType);
    }

    private <T> T post(String path, BodyPublisher body, String contentType, Class<T> responseType) {
        HttpRequest.Builder builder = request(path).POST(body);
        if (contentType != null) builder.header("Content-Type", contentType);
        return send(builder, responseType);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(REQUEST_TIMEOUT);
    }

    private <T> T send(HttpRequest.Builder builder, Class<T> responseType) {
        HttpResponse<String> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ServerException(0, "Cannot reach the server at " + baseUrl + ".");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerException(0, "Request was interrupted.");
        }

        int status = response.statusCode();
        try {
            if (status >= 200 && status < 300) {
                if (responseType == Void.class) return null;
                T body = gson.fromJson(response.body(), responseType);
                if (body != null) return body;
            } else {
                ErrorDto error = gson.fromJson(response.body(), ErrorDto.class);
                if (error != null && error.error() != null) {
                    throw new ServerException(status, error.error());
                }
            }
        } catch (JsonParseException e) {
            // not one of our JSON bodies: wrong address, or a proxy/Tomcat error page
        }
        throw new ServerException(status, "Unexpected response from " + baseUrl + " (HTTP " + status + ").");
    }
}
