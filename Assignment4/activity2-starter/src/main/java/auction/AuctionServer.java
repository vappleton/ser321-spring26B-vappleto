package auction;

import buffers.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Auction Game Server - Players compete against bot opponents.
 * Each player plays independently against 3 bots.
 */
public class AuctionServer {
    private static final int DEFAULT_PORT = 8889;
    private static final String SCORES_FILE = "scores.txt";

    private static final int initialGold = 150;

    // Shared leaderboard
    private static LeaderboardManager leaderboard;

    // Track connected player names (to prevent duplicates)
    private static Set<String> activePlayerNames = Collections.synchronizedSet(new HashSet<>()); //to make  it thread-safe

    // Grading mode flag
    private static boolean gradingMode = false;

    // Bot opponent name pool
    private static final String[] BOT_NAMES = {
            "Alaric", "Brynn", "Cedric", "Daphne",
            "Elara", "Finn", "Gwen", "Hugo",
            "Isolde", "Jasper"
    };
    private static Random botNameRandom = new Random();
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--grading")) {
                gradingMode = true;
                System.out.println("Running in grading mode (deterministic results)");
            } else {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i]);
                }
            }
        }

        // Initialize leaderboard
        leaderboard = new LeaderboardManager(SCORES_FILE);
        System.out.println("Leaderboard loaded with " + leaderboard.size() + " scores");


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Auction Server started on port " + port);
            System.out.println("Waiting for connections...");

            int clientId = 0;
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientId++;
                    final int id = clientId;
                    System.out.println("Client " + id + " connected from " +
                            clientSocket.getInetAddress().getHostAddress());

                    threadPool.execute(() -> processConnection(clientSocket, id)); //the thread pool
                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Handle a client connection (runs in thread pool).
     */
    private static void processConnection(Socket clientSocket, int clientId) {
        String playerName = null;
        PlayerGameState gameState = null;

        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            System.out.println("[Client " + clientId + "] Handler started");

            // Send initial welcome
            sendWelcome(out, "Welcome to the Auction Game! Please set your name.");

            // Read and process requests
            Request request;
            while ((request = Request.parseDelimitedFrom(in)) != null) {
                Request.RequestType type = request.getType();
                System.out.println("[Client " + clientId + "] Received: " + type);

                Response response = null;

                switch (type) {
                    case REGISTER:
                        String[] result = handleRegister(request, playerName);
                        playerName = result[0];
                        String message = result[1];
                        if (playerName != null) {
                            response = buildWelcome("Welcome, " + playerName + "! You have " + initialGold + " gold. " +
                                    "Type 'join' to start playing against bot opponents!");
                        } else {
                            response = buildError(message);
                        }
                        break;
                    case QUIT:
                        response = handleQuit(gameState);
                        if (response != null) {
                            response.writeDelimitedTo(out);
                        }
                        return; // Exit handler
                    case  JOIN:
                        if (playerName == null) {
                            response = buildError("Please enter your name first");
                            break;
                        }
                        if (gameState != null) {
                            response = buildError("You are already in a game");
                            break;
                        }
                        gameState = new PlayerGameState(playerName, gradingMode);
                        response = handleJoin(gameState);
                        break;

                    case BID:
                        if (gameState ==null) {
                            response = buildError("You need to join a game first!");
                            break;
                        }
                        response = handleBid(request, gameState);
                        break;

                    case LEADERBOARD:
                        response = handleLeaderboard();
                        break;

                    default:
                        response = buildError("Unknown request type");
                }

                if (response != null) {
                    response.writeDelimitedTo(out);

                    //if theres no next item, send game over
                    if (response.getType() ==Response.ResponseType.BID_RESULT &&
                        !response.hasNextItem()) {
                        Response gameOver = handleGameOver(gameState);
                        gameOver.writeDelimitedTo(out);
                    }
                }
            }

            System.out.println("[Client " + clientId + "] Disconnected");

        } catch (IOException e) {
            System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
        } finally {
            // Cleanup
            if (playerName != null) {
                activePlayerNames.remove(playerName);
                System.out.println("[Client " + clientId + "] Removed player: " + playerName);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    //handle Join request
    private static Response handleJoin(PlayerGameState gameState) {
        Item firstItem = gameState.getCurrentItem(); //get first item

        AuctionItem protoItem = itemToProto(firstItem); //converting to protobuf

        PlayerStatus status = PlayerStatus.newBuilder() //build the player's status
                .setGoldRemaining(gameState.getGold())
                .build();

        String message = "Game started! You're playing against " +
                gameState.getBot1().getName() + ", " +
                gameState.getBot2().getName() + ", " +
                gameState.getBot3().getName() + ". Current item: ";


        return Response.newBuilder() //building the resopnse
                .setType(Response.ResponseType.GAME_JOINED)
                .setOk(true)
                .setMessage(message)
                .setPlayerStatus(status)
                .setNextItem(protoItem)
                .build();


    }

    private static Response handleBid(Request request, PlayerGameState gameState) {

        int bidAmount = request.getBidAmount();
        int itemId = request.getItemId();

        Item currentItem = gameState.getCurrentItem();

        /// /////////////validate the bid//////////////////
        String error = gameState.validateBid(itemId, bidAmount);
        if (error != null) {
            return buildError(error);
        }
        ////////// /treat -1 as 0 (as skip bid)///////////
        int playerBid = (bidAmount == -1) ? 0 : bidAmount;

        //////// /////generatie the bids//////////////////
        //player bid
        Map<String, Integer> allBids = new HashMap<>();
        allBids.put(gameState.getPlayerName(), playerBid);

        //bots
        int bot1Bid = gameState.getBot1().decideBid(currentItem);
        int bot2Bid = gameState.getBot2().decideBid(currentItem);
        int bot3Bid = gameState.getBot3().decideBid(currentItem);

        allBids.put (gameState.getBot1().getName(), bot1Bid);
        allBids.put(gameState.getBot2().getName(), bot2Bid);
        allBids.put(gameState.getBot3().getName(), bot3Bid);

        /// //////////determine the winner/////////////////////

        int reservePrice = currentItem.getMinValue() /2;

        String winner = "(unsold))";
        int highestBid = 0;

        //find the highest bid
        for (Map.Entry<String, Integer> entry : allBids.entrySet()) {
            int bid = entry.getValue();

            if (bid >= reservePrice) {
                if (bid > highestBid) {
                    highestBid = bid;
                    winner = entry.getKey();
                } else  if (bid == highestBid && bid > 0) {
                    //break the tie alphabetically
                    if (entry.getKey().compareTo(winner) < 0) {
                        winner = entry.getKey();
                    }

                }
            }
        }
        /// //////////////// Award the item /////////////////////
        if (winner.equals(gameState.getPlayerName())) {
            gameState.awardItemToPlayer(currentItem, highestBid);
        }
        else if (winner.equals(gameState.getBot1().getName())) {
            gameState.bot1Items.add(currentItem);
            gameState.bot1Gold -= highestBid;
        }
        else if (winner.equals(gameState.getBot2().getName())) {
            gameState.bot2Items.add(currentItem);
            gameState.bot2Gold -= highestBid;
        }
        else if (winner.equals(gameState.getBot3().getName())) {
            gameState.bot3Items.add(currentItem);
            gameState.bot3Gold -= highestBid;
        }

        /// /////////build BID_RESULT response/////////////////
        List<PlayerBid> bidResultList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : allBids.entrySet()) {
            bidResultList.add(PlayerBid.newBuilder()
                    .setPlayerName(entry.getKey())
                    .setBidAmount(entry.getValue())
                    .build());

        }

        AuctionResult  finalAuctionResults = AuctionResult.newBuilder()
                .setItem(itemToProto(currentItem))
                .setActualValue(currentItem.getActualValue())
                .setWinnerName(winner)
                .setWinningBid(highestBid)
                .addAllAllBids(bidResultList)
                .build();

        Response.Builder responseBuilder = Response.newBuilder()
                .setType(Response.ResponseType.BID_RESULT)
                .setOk(true)
                .setMessage("Auction complete!")
                .setResult(finalAuctionResults)
                .setPlayerStatus(PlayerStatus.newBuilder()
                        .setGoldRemaining(gameState.getGold())
                        .build()
                );

        /// /////////including next item if more remain/////////////////

        boolean remainingItem = gameState.moveToNextItem();

        if (remainingItem) {
            responseBuilder.setNextItem(itemToProto(gameState.getCurrentItem()));

        }
        return responseBuilder.build();


    }

    private static Response handleGameOver(PlayerGameState gameState) {

        /// ///////////player scores///////////////////
        int playerGold = gameState.getGold();
        int playerItemsValue = gameState.getInventoryValue();
        int playerTotal = playerGold + playerItemsValue;

        int bot1Gold = gameState.bot1Gold;
        int bot1ItemsValue = getItemsValue(gameState.bot1Items);
        int bot1total = bot1Gold + bot1ItemsValue;

        int bot2Gold = gameState.bot2Gold;
        int bot2ItemsValue = getItemsValue(gameState.bot2Items);
        int bot2total = bot2Gold + bot2ItemsValue;

        int bot3Gold = gameState.bot3Gold;
        int bot3ItemsValue = getItemsValue(gameState.bot3Items);
        int bot3total = bot3Gold + bot3ItemsValue;



        ////////////////////////determine game winner /////////////////////////

        Map<String, Integer> scores = new HashMap<>();
        scores.put(gameState.getPlayerName(), playerTotal);
        scores.put(gameState.getBot1().getName(), bot1total);
        scores.put(gameState.getBot2().getName(), bot2total);
        scores.put(gameState.getBot3().getName(), bot3total);

        String winner = "";
        int bestScore = -1;

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String name = entry.getKey();
            int score = entry.getValue();

            if (score > bestScore) {
                bestScore = score;
                winner = name;
            } else if (score == bestScore) {
                if(name.compareTo(winner) < 0) {
                    winner = name;
                }
            }
        }
        //////////////////add to leaderboar////////////////////////////
        int rank = leaderboard.addScore(gameState.getPlayerName(),playerTotal);

        List<PlayerStatus> players = new ArrayList<>();

        //player
        players.add(PlayerStatus.newBuilder()
                .setPlayerName(gameState.getPlayerName())
                .setGoldRemaining(playerGold)
                .setItemsValue(playerItemsValue)
                .setTotalScore(playerTotal)
                .addAllItemsWon(gameState.getItemNames())
                .build());
        //bot1
        players.add(PlayerStatus.newBuilder()
                .setPlayerName(gameState.getBot1().getName())
                .setGoldRemaining(bot1Gold)
                .setItemsValue(bot1ItemsValue)
                .setTotalScore(bot1total)
                .build());

        //bot2
        players.add(PlayerStatus.newBuilder()
                .setPlayerName(gameState.getBot2().getName())
                .setGoldRemaining(bot2Gold)
                .setItemsValue(bot2ItemsValue)
                .setTotalScore(bot2total)
                .build());

        //bot3
        players.add(PlayerStatus.newBuilder()
                .setPlayerName(gameState.getBot3().getName())
                .setGoldRemaining(bot3Gold)
                .setItemsValue(bot3ItemsValue)
                .setTotalScore(bot3total)
                .build());

        //build final response
        GameResult finalResult = GameResult.newBuilder()
                .addAllPlayerScores(players)
                .setWinnerName(winner)
                .setLeaderboardPosition(rank)
                .build();
        return Response.newBuilder()
                .setType(Response.ResponseType.GAME_OVER)
                .setOk(true)
                .setMessage("Game Over! Final results: ")
                .setGameResult(finalResult)
                .build();
    }

    //helper method for the bot's inventory value
    private static int getItemsValue(List<Item> items) {
        int total =0;
        for (Item item : items) {
            total += item.getActualValue();
        }
        return total;
    }

    private static Response handleLeaderboard() {

        List<LeaderboardEntry> topScores = leaderboard.getTopScores(10);

        Leaderboard protoLeaderboard = Leaderboard.newBuilder()
                .addAllEntries(topScores)
                .build();

        return Response.newBuilder()
                .setType(Response.ResponseType.LEADERBOARD_RESPONSE)
                .setOk(true)
                .setMessage("Top 10 scores:")
                .setLeaderboard(protoLeaderboard)
                .build();
    }


    /**
     * Handle REGISTER request - set player name.
     * Returns [playerName, errorMessage] - playerName is null if error.
     */
    private static String[] handleRegister(Request request, String currentName) {
        String name = request.getName().trim();

        if (name.isEmpty()) {
            return new String[]{null, "Name cannot be empty"};
        }

        if (activePlayerNames.contains(name)) {
            return new String[]{null, "Name already taken. Please choose another."};
        }

        // Add new name
        activePlayerNames.add(name);
        return new String[]{name, null};
    }

    /**
     * Handle QUIT request.
     */
    private static Response handleQuit(PlayerGameState gameState) {
        String message = "Thanks for playing!";
        if (gameState != null) {
            message += " Final score: " + gameState.getPlayerScore() + ".";
        }
        message += " Goodbye!";

        return Response.newBuilder()
                .setType(Response.ResponseType.FAREWELL)
                .setOk(true)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: send welcome response.
     */
    private static void sendWelcome(OutputStream out, String message) throws IOException {
        buildWelcome(message).writeDelimitedTo(out);
    }

    /**
     * Helper: build welcome response.
     */
    private static Response buildWelcome(String message) {
        return Response.newBuilder()
                .setType(Response.ResponseType.WELCOME)
                .setOk(true)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: build error response.
     */
    private static Response buildError(String message) {
        return Response.newBuilder()
                .setType(Response.ResponseType.ERROR)
                .setOk(false)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: convert Item to protobuf AuctionItem.
     * Includes reserve_price calculated as 50% of min_value.
     */
    private static AuctionItem itemToProto(Item item) {
        return AuctionItem.newBuilder()
                .setId(item.getId())
                .setName(item.getName())
                .setCategory(item.getCategory())
                .setMinValue(item.getMinValue())
                .setMaxValue(item.getMaxValue())
                .setReservePrice(item.getMinValue() / 2)
                .build();
    }

    /**
     * Helper: get random bot name.
     */
    private static String getRandomBotName() {
        return BOT_NAMES[botNameRandom.nextInt(BOT_NAMES.length)];
    }

    /**
     * Inner class to track player game state.
     */
    private static class PlayerGameState {
        private String playerName;
        private int gold;
        private List<Item> inventory;
        private List<Item> items;
        private int currentItemIndex;
        private BotOpponent bot1;
        private BotOpponent bot2;
        private BotOpponent bot3;

        //variables to track bot outcomes
        private int bot1Gold;
        private int bot2Gold;
        private int bot3Gold;

        private List<Item> bot1Items;
        private List<Item> bot2Items;
        private List<Item> bot3Items;

        public PlayerGameState(String playerName, boolean gradingMode) {
            this.playerName = playerName;
            this.gold = initialGold;
            this.inventory = new ArrayList<>();

            // Load items
            this.items = ItemLoader.loadItems(gradingMode);
            this.currentItemIndex = 0;

            // Initialize bot tracking
            this.bot1Gold = initialGold;
            this.bot2Gold = initialGold;
            this.bot3Gold = initialGold;

            this.bot1Items = new ArrayList<>();
            this.bot2Items = new ArrayList<>();
            this.bot3Items = new ArrayList<>();

            // Create 3 bot opponents with unique names
            Set<String> usedNames = new HashSet<>();
            this.bot1 = createUniqueBot(usedNames, gradingMode);
            this.bot2 = createUniqueBot(usedNames, gradingMode);
            this.bot3 = createUniqueBot(usedNames, gradingMode);


        }

        private BotOpponent createUniqueBot(Set<String> usedNames, boolean gradingMode) {
            String name;
            do {
                name = getRandomBotName();
            } while (usedNames.contains(name));
            usedNames.add(name);
            return new BotOpponent(name, gradingMode);
        }

        /**
         * Validate a bid.
         * Returns null if valid, error message if invalid.
         * bid_amount of -1 means skip (treated as bid of 0).
         * Bids > 0 must meet the reserve price.
         */
        public String validateBid(int itemId, int bidAmount) {
            Item currentItem = getCurrentItem();

            if (currentItem.getId() != itemId) {
                return "Invalid item ID. Current item is #" + currentItem.getId();
            }

            // -1 means skip
            if (bidAmount == -1) {
                return null; // Valid skip
            }

            if (bidAmount < 0) {
                return "Bid cannot be negative (use -1 to skip)";
            }

            if (bidAmount > gold) {
                return "Insufficient gold. You have " + gold + " gold.";
            }

            // Check reserve price (bids > 0 must meet reserve)
            int reservePrice = currentItem.getMinValue() / 2;
            if (bidAmount > 0 && bidAmount < reservePrice) {
                return "Bid must meet reserve price of " + reservePrice + " gold.";
            }

            return null; // Valid
        }

        public void awardItemToPlayer(Item item, int bidAmount) {
            inventory.add(item);
            gold -= bidAmount;
        }

        public boolean moveToNextItem() {
            currentItemIndex++;
            return currentItemIndex < items.size();
        }

        public Item getCurrentItem() {
            return items.get(currentItemIndex);
        }

        public int getInventoryValue() {
            int total = 0;
            for (Item item : inventory) {
                total += item.getActualValue();
            }
            return total;
        }

        public int getPlayerScore() {
            return gold + getInventoryValue();
        }

        public List<String> getItemNames() {
            List<String> names = new ArrayList<>();
            for (Item item : inventory) {
                names.add(item.getName());
            }
            return names;
        }

        // Getters
        public String getPlayerName() { return playerName; }
        public int getGold() { return gold; }
        public List<Item> getInventory() { return new ArrayList<>(inventory); }
        public BotOpponent getBot1() { return bot1; }
        public BotOpponent getBot2() { return bot2; }
        public BotOpponent getBot3() { return bot3; }
    }
}
