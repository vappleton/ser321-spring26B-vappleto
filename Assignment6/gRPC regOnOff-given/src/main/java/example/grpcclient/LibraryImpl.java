package example.grpcclient;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.json.JSONArray;
import org.json.JSONObject;
import service.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LibraryImpl extends LibraryGrpc.LibraryImplBase {
    private Map<String, BookRecord> books = new HashMap<>();

    public LibraryImpl() {
        loadData();
    }
    private void loadData() {
        try {
            File file = new File("library_data.json");

            if (!file.exists() || file.length() == 0) {
                loadFromInitialFile(); //books.json
                saveData();
                return;
            }

            String content = new String(Files.readAllBytes(file.toPath()));
            JSONArray jsonArray = new JSONArray(content);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                BookRecord bookEntry = new BookRecord(
                        jsonObject.getString("title"),
                        jsonObject.getString("author"),
                        jsonObject.getString("isbn")
                );
                bookEntry.isBorrowed = jsonObject.getBoolean("isBorrowed");
                bookEntry.borrowedBy = jsonObject.getString("borrowedBy");
                books.put(bookEntry.isbn, bookEntry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void saveData() {
        JSONArray jsonArray = new JSONArray();

        for (BookRecord book : books.values()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title", book.title);
            jsonObject.put("author", book.author);
            jsonObject.put("isbn", book.isbn);
            jsonObject.put("isBorrowed", book.isBorrowed);
            jsonObject.put("borrowedBy", book.borrowedBy);

            jsonArray.put(jsonObject);

        }
        try {
            FileWriter writer = new FileWriter("library_data.json");
            writer.write(jsonArray.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //load initial file books.json
    private void loadFromInitialFile() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("books.json"));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray jsonArray = new JSONArray(sb.toString());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String title = jsonObject.getString("title");
                String author = jsonObject.getString("author");
                String isbn = jsonObject.getString("isbn");

                BookRecord book = new BookRecord(title, author, isbn);
                books.put(isbn, book);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //now implementing rpcs

    //listBooks

    @Override
    public void listBooks(Empty req,StreamObserver<BookListResponse> responseObserver) {

        if (books.isEmpty()) {
            responseObserver.onNext(BookListResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("no books in library yet")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        BookListResponse.Builder response = BookListResponse.newBuilder().setIsSuccess(true);

        for (BookRecord book : books.values()) {
            response.addBooks(convertToProto(book));
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    //searchBooks
    @Override
    public void searchBooks(BookSearchRequest request, StreamObserver<BookListResponse> responseObserver) {

        String query = request.getQuery().toLowerCase();

        List<Book> results = new ArrayList<>();

        for (BookRecord book : books.values()) {
            if(book.title.toLowerCase().contains(query) || book.author.toLowerCase().contains(query)) {
                results.add(convertToProto(book));
            }
        }
        if (results.isEmpty()) {
            responseObserver.onNext(BookListResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("no books found matching query")
                    .build());
        } else {
            responseObserver.onNext(BookListResponse.newBuilder()
                    .setIsSuccess(true)
                    .addAllBooks(results)
                    .build());
        }

        responseObserver.onCompleted();
    }
    //borrowBook
    @Override
    public void borrowBook(BorrowRequest request, StreamObserver<BorrowResponse> responseObserver) {
        String isbn = request.getIsbn();
        String name = request.getBorrowerName();

        if (name.isEmpty()) {
            responseObserver.onNext(BorrowResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("borrower name is required")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (isbn.isEmpty()) {
            responseObserver.onNext(BorrowResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("missing field")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        if (!books.containsKey(isbn)) {
            responseObserver.onNext(BorrowResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("book not found")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        BookRecord book = books.get(isbn);

        if (book.isBorrowed) {
            responseObserver.onNext(BorrowResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("book is already borrowed")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        book.isBorrowed = true;
        book.borrowedBy = name;

        saveData();

        responseObserver.onNext(BorrowResponse.newBuilder()
                .setIsSuccess(true)
                .setMessage("book borrowed successfully")
                .build());

        responseObserver.onCompleted();
    }

    //return book
    @Override
    public void returnBook(ReturnRequest request, StreamObserver<ReturnResponse> responseObserver) {
        String isbn = request.getIsbn();

        if (isbn.isEmpty()) {
            responseObserver.onNext(ReturnResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("missing field")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        if (!books.containsKey(isbn)) {
            responseObserver.onNext(ReturnResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("book not found")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        BookRecord book = books.get(isbn);

        if (!book.isBorrowed) {
            responseObserver.onNext(ReturnResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("book is not borrowed")
                    .build());
            responseObserver.onCompleted();
            return;
        }
        book.isBorrowed = false;
        book.borrowedBy ="";

        saveData();

        responseObserver.onNext(ReturnResponse.newBuilder()
                .setIsSuccess(true)
                .setMessage("book returned successfully")
                .build());

        responseObserver.onCompleted();

    }

    //helper to convert the BookRecord object into a gRPC/protobuf Book
    private Book convertToProto(BookRecord b) {
        return Book.newBuilder()
                .setTitle(b.title)
                .setAuthor(b.author)
                .setIsbn(b.isbn)
                .setIsBorrowed(b.isBorrowed)
                .setBorrowedBy(b.borrowedBy)
                .build();
    }



}
