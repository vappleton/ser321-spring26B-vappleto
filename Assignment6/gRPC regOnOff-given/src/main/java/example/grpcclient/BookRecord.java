package example.grpcclient;

public class BookRecord {
    String title;
    String author;
    String isbn;
    boolean isBorrowed;
    String borrowedBy;

    public BookRecord(String title, String author, String isbn) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.isBorrowed = false;
        this.borrowedBy = "";
    }
}
