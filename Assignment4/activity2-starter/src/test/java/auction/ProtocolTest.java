package auction;

import buffers.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.Socket;

/**
 * Protocol compliance tests for AuctionServer.
 *
 * These tests verify that the protocol is followed.
 *
 * USAGE:
 * 1. Start your server: gradle runServer --args="--grading"
 * 2. Run tests: gradle test
 *
 * Tests use grading mode for deterministic results.
**/

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProtocolTest {

    private static final String HOST = "localhost";
    private static final int PORT = 8889;
    private static final int TIMEOUT = 5000; // 5 second timeout

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    /**
     * Connect to server before each test.
     */
    @BeforeEach
    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        socket.setSoTimeout(TIMEOUT);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    /**
     * Disconnect after each test.
     */
    @AfterEach
    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Test 1: Initial Connection
     * Server should send WELCOME message on connect.
     */
    @Test
    @Order(1)
    public void testInitialWelcome() throws IOException {
        Response welcome = Response.parseDelimitedFrom(in);

        assertNotNull(welcome, "Server should send welcome message on connect");
        assertEquals(Response.ResponseType.WELCOME, welcome.getType(),
            "Initial message should be WELCOME");
        assertTrue(welcome.getOk(), "Welcome should have ok=true");
        assertTrue(welcome.getMessage().toLowerCase().contains("welcome"),
            "Welcome message should contain 'welcome'");
    }

    /**
     * Test 2: REGISTER Request - Valid Name
     * Server should accept valid name and send WELCOME with gold amount.
     */
    @Test
    @Order(2)
    public void testRegisterRequestValid() throws IOException {
        // Read initial welcome
        Response.parseDelimitedFrom(in);

        // Send REGISTER request
        Request registerRequest = Request.newBuilder()
            .setType(Request.RequestType.REGISTER)
            .setName("TestPlayer")
            .build();
        registerRequest.writeDelimitedTo(out);

        // Read response
        Response response = Response.parseDelimitedFrom(in);

        assertNotNull(response, "Server should respond to REGISTER request");
        assertEquals(Response.ResponseType.WELCOME, response.getType(),
            "Response should be WELCOME");
        assertTrue(response.getOk(), "REGISTER request should succeed");
        assertTrue(response.getMessage().contains("TestPlayer"),
            "Response should include player name");
        assertTrue(response.getMessage().contains("150") || response.getMessage().contains("gold"),
            "Response should mention starting gold");
    }

    /**
     * Test 3: REGISTER Request - Empty Name
     * Server should reject empty names with ERROR.
     */
    @Test
    @Order(3)
    public void testRegisterRequestEmpty() throws IOException {
        Response.parseDelimitedFrom(in); // Initial welcome

        Request registerRequest = Request.newBuilder()
            .setType(Request.RequestType.REGISTER)
            .setName("")
            .build();
        registerRequest.writeDelimitedTo(out);

        Response response = Response.parseDelimitedFrom(in);

        assertNotNull(response);
        assertEquals(Response.ResponseType.ERROR, response.getType(),
            "Empty name should return ERROR");
        assertFalse(response.getOk(), "Error response should have ok=false");
        assertTrue(response.getMessage().toLowerCase().contains("empty"),
            "Error should mention empty name");
    }

    /**
     * Test 4: QUIT Request
     * Server should send FAREWELL response.
     */
    @Test
    @Order(4)
    public void testQuitRequest() throws IOException {
        Response.parseDelimitedFrom(in);

        Request quitRequest = Request.newBuilder()
            .setType(Request.RequestType.QUIT)
            .build();
        quitRequest.writeDelimitedTo(out);

        Response response = Response.parseDelimitedFrom(in);

        assertEquals(Response.ResponseType.FAREWELL, response.getType());
        assertTrue(response.getOk());
        assertTrue(response.getMessage().toLowerCase().contains("goodbye") ||
                   response.getMessage().toLowerCase().contains("bye"),
            "FAREWELL message should be friendly");
    }

    // ====================================================================
    // ADD YOUR TESTS BELOW (at least 3 more tests required)
    // ====================================================================

    @Test
    @Order(5)
    public void testJoinRequest() throws IOException {
        Response.parseDelimitedFrom(in);

       sendRegister("JoinTestPlayer");

       sendJoin();

       Response response = Response.parseDelimitedFrom(in);
       assertNotNull(response);
       assertEquals(Response.ResponseType.GAME_JOINED, response.getType());
       assertTrue(response.getOk());

       assertTrue(response.hasPlayerStatus(), "Should include player status");
       assertEquals(150, response.getPlayerStatus().getGoldRemaining(), "the player should start with 150 gold");
       assertTrue(response.hasNextItem(), "should have the first auction item");

    }

    @Test
    @Order(6)
    public void testValidBidRequest() throws IOException {
        Response.parseDelimitedFrom(in);
        sendRegister("ValidBidTestPlayer");
        sendJoin();
        //read game_joined repsonse
        Response joinResponse = Response.parseDelimitedFrom(in);
        assertNotNull(joinResponse);
        assertEquals(Response.ResponseType.GAME_JOINED, joinResponse.getType());

        int itemId = joinResponse.getNextItem().getId();
        int reserve = joinResponse.getNextItem().getReservePrice();

        //send a valid bit at reserve price
        Request bidRequest = Request.newBuilder()
                .setType(Request.RequestType.BID)
                .setItemId(itemId)
                .setBidAmount(reserve)
                .build();
        bidRequest.writeDelimitedTo(out);
        Response response = Response.parseDelimitedFrom(in);

        assertNotNull(response);
        assertEquals(Response.ResponseType.BID_RESULT, response.getType(), "a valid bid should return BID_RESULT");

        assertTrue(response.getOk(), "a valid bid should succedd");

        assertTrue(response.hasResult(), "Response should includ ethe auction results");
        assertTrue(response.hasPlayerStatus(), "Response should includ player status");


    }
    @Test
    @Order(7)
    public void testInvalidBidRequest() throws IOException {
        Response.parseDelimitedFrom(in);
        sendRegister("InvalidBidTestPlayer");
        sendJoin();
        Response joinResponse = Response.parseDelimitedFrom(in);
        int itemId = joinResponse.getNextItem().getId();

        //sending an invalid bid
        Request bidRequest = Request.newBuilder()
                .setType(Request.RequestType.BID)
                .setItemId(itemId)
                .setBidAmount(1) //testing with 1 first to see if its below reserve
                .build();
        bidRequest.writeDelimitedTo(out);
        Response response = Response.parseDelimitedFrom(in);

        assertEquals(Response.ResponseType.ERROR, response.getType(), "an invalid bid should return ERROR");
        assertFalse(response.getOk());
        assertTrue(response.getMessage().contains("reserve"), "Error should mention reserve price");

    }
    @Test
    @Order(8)
    public void testSkipBidRequest() throws IOException {
        Response.parseDelimitedFrom(in); //welcome

        sendRegister("SkipBidTester");
        sendJoin();
        Response joinResponse = Response.parseDelimitedFrom(in);
        int itemId = joinResponse.getNextItem().getId();

        //send skip bid
        Request bidRequest = Request.newBuilder()
                .setType(Request.RequestType.BID)
                .setItemId(itemId)
                .setBidAmount(-1)
                .build();

        bidRequest.writeDelimitedTo(out);
        Response response = Response.parseDelimitedFrom(in);

        assertEquals(Response.ResponseType.BID_RESULT, response.getType(), "skip should still output BID_RESULT");
        assertTrue(response.getOk());
        assertTrue(response.hasResult(),"Should include auction results");

    }



    // Helper methods if you want to use them

    private void sendRegister(String name) throws IOException {
        Request registerRequest = Request.newBuilder()
            .setType(Request.RequestType.REGISTER)
            .setName(name)
            .build();
        registerRequest.writeDelimitedTo(out);
        Response.parseDelimitedFrom(in); // Consume response
    }

    private void sendJoin() throws IOException {
        Request joinRequest = Request.newBuilder()
            .setType(Request.RequestType.JOIN)
            .build();
        joinRequest.writeDelimitedTo(out);
    }
}
