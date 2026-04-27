import com.google.protobuf.Empty;
import example.grpcclient.Client;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import service.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Server unit tests for Assignment 6.
 *
 * IMPORTANT: These tests require the server to be running BEFORE you run them.
 *
 * To run these tests:
 * 1. First, start the server in one terminal: gradle runNode
 * 2. Then, in another terminal, run: gradle test
 *
 * The tests connect to localhost:8000 (the default port for runNode).
 * Make sure your server is running on this port before running tests.
 *
 * TODO for students:
 * This file contains example tests for the Echo and Joke services.
 * You need to add your own tests for:
 * - Converter service (happy path and error cases)
 * - Library service (happy path, error cases, and persistence testing)
 *
 * Your tests should follow the same pattern as the examples below.
 */
public class ServerTest {

    ManagedChannel channel;
    private EchoGrpc.EchoBlockingStub blockingStub;
    private JokeGrpc.JokeBlockingStub blockingStub2;
    private ConverterGrpc.ConverterBlockingStub converterStub;
    private LibraryGrpc.LibraryBlockingStub libraryStub;


    @org.junit.Before
    public void setUp() throws Exception {
        // assuming default port and localhost for our testing, make sure Node runs on this port
        channel = ManagedChannelBuilder.forTarget("localhost:8000").usePlaintext().build();

        blockingStub = EchoGrpc.newBlockingStub(channel);
        blockingStub2 = JokeGrpc.newBlockingStub(channel);
        converterStub = ConverterGrpc.newBlockingStub(channel);
        libraryStub = LibraryGrpc.newBlockingStub(channel);
    }

    @org.junit.After
    public void close() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);

    }


    @Test
    public void parrot() {
        // success case
        ClientRequest request = ClientRequest.newBuilder().setMessage("test").build();
        ServerResponse response = blockingStub.parrot(request);
        assertTrue(response.getIsSuccess());
        assertEquals("test", response.getMessage());

        // error cases
        request = ClientRequest.newBuilder().build();
        response = blockingStub.parrot(request);
        assertFalse(response.getIsSuccess());
        assertEquals("No message provided", response.getError());

        request = ClientRequest.newBuilder().setMessage("").build();
        response = blockingStub.parrot(request);
        assertFalse(response.getIsSuccess());
        assertEquals("No message provided", response.getError());
    }

    // For this test the server needs to be started fresh AND the list of jokes needs to be the initial list
    @Test
    public void joke() {
        // getting first joke
        JokeReq request = JokeReq.newBuilder().setNumber(1).build();
        JokeRes response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("Did you hear the rumor about butter? Well, I'm not going to spread it!", response.getJoke(0));

        // getting next 2 jokes
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(2, response.getJokeCount());
        assertEquals("What do you call someone with no body and no nose? Nobody knows.", response.getJoke(0));
        assertEquals("I don't trust stairs. They're always up to something.", response.getJoke(1));

        // getting 2 more but only one more on server
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(2, response.getJokeCount());
        assertEquals("How do you get a squirrel to like you? Act like a nut.", response.getJoke(0));
        assertEquals("I am out of jokes...", response.getJoke(1));

        // trying to get more jokes but out of jokes
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("I am out of jokes...", response.getJoke(0));

        // trying to add joke without joke field
        JokeSetReq req2 = JokeSetReq.newBuilder().build();
        JokeSetRes res2 = blockingStub2.setJoke(req2);
        assertFalse(res2.getOk());

        // trying to add empty joke
        req2 = JokeSetReq.newBuilder().setJoke("").build();
        res2 = blockingStub2.setJoke(req2);
        assertFalse(res2.getOk());

        // adding a new joke (well word)
        req2 = JokeSetReq.newBuilder().setJoke("whoop").build();
        res2 = blockingStub2.setJoke(req2);
        assertTrue(res2.getOk());

        // should have the new "joke" now and return it
        request = JokeReq.newBuilder().setNumber(1).build();
        response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("whoop", response.getJoke(0));
    }
    //testing length conversion happy path
    @Test
    public void testConvertKmtoMile() {
        ConversionRequest req = ConversionRequest.newBuilder()
                .setValue(1)
                .setFromUnit("KILOMETER")
                .setToUnit("MILE")
                .build();

        ConversionResponse res = converterStub.convert(req);

        assertTrue(res.getIsSuccess());
        assertEquals(0.62, res.getResult(), 0.01);
    }

    //test temperature conversion happy path
    @Test
    public void testConvertCelsiusToFahrenheit() {
        ConversionRequest req = ConversionRequest.newBuilder()
                .setValue(0)
                .setFromUnit("CELSIUS")
                .setToUnit("FAHRENHEIT")
                .build();

        ConversionResponse res = converterStub.convert(req);

        assertTrue(res.getIsSuccess());
        assertEquals(32.0, res.getResult(), 0.01);
    }
    //testing weight converison happy path
    @Test
    public void testConvertKgtoPound() {
        ConversionRequest req = ConversionRequest.newBuilder()
                .setValue(1)
                .setFromUnit("KILOGRAM")
                .setToUnit("POUND")
                .build();

        ConversionResponse res = converterStub.convert(req);

        assertTrue(res.getIsSuccess());
        assertEquals(2.20, res.getResult(), 0.01);

    }
    //error cases:
    //Missing from_unit
    @Test
    public void TestMissingFromUnit() {
        ConversionRequest req = ConversionRequest.newBuilder()
                .setValue(10)
                .setFromUnit("")
                .setToUnit("MILE")
                .build();

        ConversionResponse res = converterStub.convert(req);

        assertFalse(res.getIsSuccess());
        assertEquals("from_unit is required", res.getError());

    }
    //testing mismatched units
    @Test
    public void testMismatchedUnits() {
        ConversionRequest req = ConversionRequest.newBuilder()
                .setValue(10)
                .setFromUnit("KILOGRAM")
                .setToUnit("MILE")
                .build();

        ConversionResponse res = converterStub.convert(req);

        assertFalse(res.getIsSuccess());
    }
    //testing absolute zero vioaltion

    @Test
    public void testBelowAbsoluteZero() {
        ConversionRequest req = ConversionRequest.newBuilder()
                .setValue(-300)
                .setFromUnit("CELSIUS")
                .setToUnit("FAHRENHEIT")
                .build();

        ConversionResponse res = converterStub.convert(req);

        assertFalse(res.getIsSuccess());
    }
    //testing unsupported unit
    @Test
    public void testUnsupportedUnit() {
        ConversionRequest req = ConversionRequest.newBuilder()
                .setValue(10)
                .setFromUnit("LITER")
                .setToUnit("MILE")
                .build();

        ConversionResponse res = converterStub.convert(req);

        assertFalse(res.getIsSuccess());
        assertTrue(res.getError().contains("unsupported unit"));
    }
    //testing a negative length value
    @Test
    public void testNegativeLength() {
        ConversionRequest req = ConversionRequest.newBuilder()
                .setValue(-1)
                .setFromUnit("KILOMETER")
                .setToUnit("MILE")
                .build();

        ConversionResponse res = converterStub.convert(req);

        assertTrue(res.getIsSuccess()); // it should still convert because distance could be negative (displacement)
    }

    /// ///////////tests for the library service//////////////
    /// happy paths:
    @Test
    public void testListBooksSuccess() {
        BookListResponse res = libraryStub.listBooks(Empty.newBuilder().build());

        assertTrue(res.getIsSuccess());
        assertTrue(res.getBooksCount() > 0);
    }
    //search books
    @Test
    public void testSearchBooksSuccess() {
        BookSearchRequest req = BookSearchRequest.newBuilder()
                .setQuery("1984")
                .build();

        BookListResponse res = libraryStub.searchBooks(req);

        assertTrue(res.getIsSuccess());
        assertTrue(res.getBooksCount() > 0);
    }
    //borrow books
    @Test
    public void testBorrowBookSuccess() {
        //returning the book first to make sure it's available, otherwise the test will fail if the book was borrowed in previous runs
        ReturnRequest retReq = ReturnRequest.newBuilder()
                .setIsbn("978-0141439518")
                .build();

        libraryStub.returnBook(retReq);

        BorrowRequest req = BorrowRequest.newBuilder()
                .setIsbn("978-0141439518") // use real ISBN thats not borrowed
                .setBorrowerName("TestUser")
                .build();

        BorrowResponse res = libraryStub.borrowBook(req);

        assertTrue(res.getIsSuccess());
    }
    //return book
    @Test
    public void testReturnBookSuccess() {
        String isbn = "978-0451524935";

        // borrow the bookfirst
        libraryStub.borrowBook(
                BorrowRequest.newBuilder()
                        .setIsbn(isbn)
                        .setBorrowerName("TestUser")
                        .build()
        );

        ReturnResponse res = libraryStub.returnBook(
                ReturnRequest.newBuilder()
                        .setIsbn(isbn)
                        .build()
        );

        assertTrue(res.getIsSuccess());
    }

    /// /////not so happy paths//////////////
    //missing borrower name
    @Test
    public void testBorrowMissingName() {
        BorrowRequest req = BorrowRequest.newBuilder()
                .setIsbn("978-0451524935")
                .setBorrowerName("")
                .build();

        BorrowResponse res = libraryStub.borrowBook(req);

        assertFalse(res.getIsSuccess());
        assertEquals("borrower name is required", res.getError());
    }

    // book not found
    @Test
    public void testBorrowBookNotFound() {
        BorrowRequest req = BorrowRequest.newBuilder()
                .setIsbn("012345")
                .setBorrowerName("User")
                .build();

        BorrowResponse res = libraryStub.borrowBook(req);

        assertFalse(res.getIsSuccess());
        assertEquals("book not found", res.getError());
    }
    //book was already borrowed
    @Test
    public void testBorrowAlreadyBorrowed() {
        String isbn = "978-0451524935";

        libraryStub.borrowBook(
                BorrowRequest.newBuilder()
                        .setIsbn(isbn)
                        .setBorrowerName("User1")
                        .build()
        );

        BorrowResponse res = libraryStub.borrowBook(
                BorrowRequest.newBuilder()
                        .setIsbn(isbn)
                        .setBorrowerName("User2")
                        .build()
        );
        assertFalse(res.getIsSuccess());
        assertEquals("book is already borrowed", res.getError());
    }
    @Test
    //persistence test
    public void testBorrowPersistence() {
        String isbn = "978-0451524935";

        // borrow book
        libraryStub.borrowBook(
                BorrowRequest.newBuilder()
                        .setIsbn(isbn)
                        .setBorrowerName("PersistUser")
                        .build()
        );

        // simulates "after restart" by just calling list again
        BookListResponse res = libraryStub.listBooks(Empty.newBuilder().build());

        boolean found = false;

        for (Book b : res.getBooksList()) {
            if (b.getIsbn().equals(isbn) && b.getIsBorrowed()) {
                found = true;
            }
        }

        assertTrue(found);
    }









}