package server;

import dto.ChatMessageDto;
import dto.CreateEventRequest;
import dto.EventDto;
import dto.EventStatus;
import dto.EventType;
import dto.FillDto;
import dto.OptionDto;
import dto.OptionStatsDto;
import dto.OrderBookDto;
import dto.OrderDto;
import dto.OrderRequest;
import dto.OrderResultDto;
import dto.PollResponse;
import dto.TradeDto;
import dto.UploadResultDto;
import dto.UserDto;
import engine.Event;
import engine.GuessMarketEngine;
import engine.MethodType;
import engine.Option;
import engine.Order;
import engine.OrderBook;
import engine.OrderBookConfig;
import engine.OrderBookTrade;
import engine.OrderSide;
import engine.OrderSubmissionResult;
import engine.Trade;
import engine.User;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

// the single in-memory system state, shared by every servlet; every method is synchronized so
// concurrent requests see and change the engine one at a time. Nothing here touches the disk.
public final class MarketService {

    static final int MAX_USERNAME_LENGTH = 30;
    static final int MAX_CHAT_LENGTH = 500;
    private static final int MAX_CHAT_HISTORY = 1000;

    private final GuessMarketEngine engine = new GuessMarketEngine();
    private final Set<String> onlineUsers = new HashSet<>();
    private final Deque<ChatMessageDto> chat = new ArrayDeque<>();
    private long chatSeq = 0;
    // bumped on every change to events or users; lets a polling client skip an unchanged snapshot
    private long version = 1;

    // a name is taken while its user is connected; a disconnected user's name reopens their account
    public synchronized String login(String requestedName) {
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("User name is required.");
        }
        if (name.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("User name must be at most " + MAX_USERNAME_LENGTH + " characters.");
        }

        User existing = findUserIgnoreCase(name);
        if (existing != null && onlineUsers.contains(existing.getName())) {
            throw new IllegalStateException("User name '" + existing.getName() + "' is already taken.");
        }
        User user = existing != null ? existing : engine.registerUser(name);
        onlineUsers.add(user.getName());
        version++;
        return user.getName();
    }

    public synchronized void logout(String username) {
        if (username != null && onlineUsers.remove(username)) {
            version++;
        }
    }

    public synchronized PollResponse poll(long knownVersion, long chatCursor) {
        List<ChatMessageDto> newMessages = new ArrayList<>();
        for (ChatMessageDto message : chat) {
            if (message.seq() > chatCursor) newMessages.add(message);
        }
        if (knownVersion == version) {
            return new PollResponse(version, false, null, null, newMessages, chatSeq);
        }

        List<EventDto> events = new ArrayList<>();
        for (Event event : engine.getEvents()) events.add(toDto(event));
        List<UserDto> users = new ArrayList<>();
        for (User user : engine.getUsers()) users.add(toDto(user));
        return new PollResponse(version, true, events, users, newMessages, chatSeq);
    }

    // the uploader becomes the market maker of every event in the file
    public synchronized UploadResultDto upload(String username, InputStream xml) throws Exception {
        User uploader = requireUser(username);
        List<Event> added = engine.addEventsFromXml(xml, uploader);
        version++;
        return new UploadResultDto(added.stream().map(Event::getName).toList());
    }

    public synchronized EventDto createEvent(String username, CreateEventRequest request) {
        User user = requireUser(username);
        if (request == null || request.type() == null) {
            throw new IllegalArgumentException("Market type is required.");
        }
        Event event = engine.createEvent(user, request.name(), request.description(), request.commissionPercent(),
                request.commissionType(), request.optionNames(), MethodType.valueOf(request.type().name()),
                request.lmsrB(), request.orderBookD(), request.orderBookInitial(), request.allowMint());
        version++;
        return toDto(event);
    }

    public synchronized void openEvent(String username, int eventId) {
        User user = requireUser(username);
        Event event = requireEvent(eventId);
        mutate(() -> {
            event.open(user);
            return null;
        });
    }

    public synchronized void closeEvent(String username, int eventId, String winningOption) {
        User user = requireUser(username);
        Event event = requireEvent(eventId);
        mutate(() -> {
            event.closeEvent(user, winningOption);
            return null;
        });
    }

    public synchronized TradeDto trade(String username, int eventId, String option, int quantity) {
        User user = requireUser(username);
        Event event = requireEvent(eventId);
        if (event.getEventType() != MethodType.LMSR) {
            throw new IllegalArgumentException("'" + event.getName() + "' is not an LMSR event.");
        }
        if (!event.getOptionNames().contains(option)) {
            throw new IllegalArgumentException("Unknown option: " + option);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        return mutate(() -> toDto(event.executeTrade(user, option, quantity)));
    }

    public synchronized OrderResultDto submitOrder(String username, int eventId, OrderRequest request) {
        User user = requireUser(username);
        Event event = requireEvent(eventId);
        if (event.getEventType() != MethodType.ORDER_BOOK) {
            throw new IllegalArgumentException("'" + event.getName() + "' is not an Order Book event.");
        }
        if (request == null || request.side() == null || request.option() == null) {
            throw new IllegalArgumentException("Option and side are required.");
        }
        OrderBook orderBook = event.getOrderBook();
        if (orderBook == null) {
            throw new IllegalStateException("Event is not active; cannot submit orders.");
        }
        return mutate(() -> {
            OrderSubmissionResult result = orderBook.submitOrder(user, request.option(),
                    OrderSide.valueOf(request.side().name()), request.quantity(), request.price());
            return new OrderResultDto(result.filledQuantity(), result.restingQuantity(), result.newFills().size());
        });
    }

    public synchronized void deposit(String username, double amount) {
        engine.deposit(requireUser(username), amount);
        version++;
    }

    public synchronized void sendChat(String username, String rawText) {
        requireUser(username);
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Message is empty.");
        }
        if (text.length() > MAX_CHAT_LENGTH) {
            throw new IllegalArgumentException("Message must be at most " + MAX_CHAT_LENGTH + " characters.");
        }
        chat.addLast(new ChatMessageDto(++chatSeq, username, text, System.currentTimeMillis()));
        if (chat.size() > MAX_CHAT_HISTORY) chat.removeFirst();
    }

    // a rejected action can still change state (insufficient funds locks the user), so the version
    // moves whether or not the action succeeds
    private <T> T mutate(Supplier<T> action) {
        try {
            return action.get();
        } finally {
            version++;
        }
    }

    private User requireUser(String username) {
        User user = username == null ? null : engine.getUserByName(username);
        if (user == null) {
            throw new SecurityException("Not logged in.");
        }
        return user;
    }

    private Event requireEvent(int eventId) {
        Event event = engine.getEventById(eventId);
        if (event == null) {
            throw new NoSuchElementException("No event with id " + eventId + ".");
        }
        return event;
    }

    private User findUserIgnoreCase(String name) {
        for (User user : engine.getUsers()) {
            if (user.getName().equalsIgnoreCase(name)) return user;
        }
        return null;
    }

    private UserDto toDto(User user) {
        int marketMakerCount = 0;
        for (Event event : engine.getEvents()) {
            if (event.getMarketMaker() == user) marketMakerCount++;
        }
        return new UserDto(user.getName(), user.getBalance(), user.isLocked(), onlineUsers.contains(user.getName()),
                marketMakerCount, new ArrayList<>(user.getParticipatedEventIds()));
    }

    private static TradeDto toDto(Trade trade) {
        return new TradeDto(trade.buyerName(), trade.optionName(), trade.quantity(),
                trade.sharesCost(), trade.commissionPaid());
    }

    private static EventDto toDto(Event event) {
        boolean lmsr = event.getEventType() == MethodType.LMSR;
        Map<String, Double> probabilities = lmsr ? event.getOptionProbabilities() : Map.of();

        List<OptionDto> options = new ArrayList<>();
        for (Option option : event.getOptions()) {
            options.add(new OptionDto(option.getOptionName(), option.getOptionQuantity(),
                    probabilities.getOrDefault(option.getOptionName(), 0.0)));
        }
        List<TradeDto> trades = new ArrayList<>();
        for (Trade trade : event.getTradeHistory()) trades.add(toDto(trade));

        return new EventDto(event.getID(), event.getName(), event.getDescription(),
                lmsr ? EventType.LMSR : EventType.ORDER_BOOK, EventStatus.valueOf(event.getState().name()),
                event.getCommission().getCommission(), event.getCommission().getType(),
                event.getMarketMaker() == null ? null : event.getMarketMaker().getName(),
                event.getAccountBalance(), event.getTotalCommissionCollected(), event.previewOpenCost(),
                event.getWinningOption(), options, lmsr ? event.getLmsrData().getB() : null,
                trades, lmsr ? null : orderBookDto(event));
    }

    private static OrderBookDto orderBookDto(Event event) {
        OrderBookConfig config = event.getOrderBookConfig();
        OrderBook book = event.getOrderBook();
        List<OptionStatsDto> stats = new ArrayList<>();
        List<OrderDto> bids = new ArrayList<>();
        List<OrderDto> asks = new ArrayList<>();
        List<FillDto> fills = new ArrayList<>();
        Map<String, Map<String, Integer>> holdings = new LinkedHashMap<>();

        if (book != null) {
            for (String option : event.getOptionNames()) {
                stats.add(new OptionStatsDto(option, book.getLast(option), book.getBestBid(option),
                        book.getBestAsk(option), book.getMid(option), book.getSpread(option)));
                for (Order order : book.getBids(option)) bids.add(toDto(order));
                for (Order order : book.getAsks(option)) asks.add(toDto(order));
            }
            for (OrderBookTrade fill : book.getFills()) {
                fills.add(new FillDto(fill.optionName(), fill.buyerName(), fill.sellerName(),
                        fill.quantity(), fill.price(), fill.commission(), fill.minted()));
            }
            for (String participant : book.getParticipantNames()) {
                holdings.put(participant, new LinkedHashMap<>(book.getHoldings(participant)));
            }
        }
        return new OrderBookDto(config.getD(), config.getInitial(), config.isAllowMint(), book != null,
                stats, bids, asks, fills, holdings);
    }

    private static OrderDto toDto(Order order) {
        return new OrderDto(order.getId(), order.getOptionName(), order.getOwnerName(),
                order.getQuantity(), order.getPrice());
    }
}
