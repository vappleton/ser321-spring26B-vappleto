package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.protobuf.Empty; // needed to use Empty


/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class Client {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final ConverterGrpc.ConverterBlockingStub converterStub; //converter stub
  private final LibraryGrpc.LibraryBlockingStub libraryStub; //library stub
  private final BucketListGrpc.BucketListBlockingStub bucketStub;

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);
    converterStub = ConverterGrpc.newBlockingStub(channel);
    libraryStub = LibraryGrpc.newBlockingStub(channel);
    bucketStub = BucketListGrpc.newBlockingStub(channel);
  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;
    converterStub = ConverterGrpc.newBlockingStub(channel);
    libraryStub = LibraryGrpc.newBlockingStub(channel);
    bucketStub = BucketListGrpc.newBlockingStub(channel);
  }

  public void askServerToParrot(String message) {

    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response;
    try {
      response = blockingStub.parrot(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    System.out.println("Received from server: " + response.getMessage());
  }

  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response;

    // just to show how to use the empty in the protobuf protocol
    Empty empt = Empty.newBuilder().build();

    try {
      response = blockingStub2.getJoke(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 6) {
      System.out
          .println("Expected arguments: <host(String)> <port(int)> <regHost(string)> <regPort(int)> <message(String)> <regOn(bool)>");
      System.exit(1);
    }
    int port = Integer.parseInt(args[1]);
    int regPort = 9003;
    String host = args[0];
    String regHost = args[2];
    String message = args[4];
    try {
      port = Integer.parseInt(args[1]);
      regPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be an integer");
      System.exit(2);
    }
      System.out.println("Host: " + host + " Port: " + port);

    // Create a communication channel to the server (Node), known as a Channel. Channels
    // are thread-safe
    // and reusable. It is common to create channels at the beginning of your
    // application and reuse
    // them until the application shuts down.
    String target = host + ":" + port;
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS
        // to avoid
        // needing certificates.
        .usePlaintext().build();

    String regTarget = regHost + ":" + regPort;
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regTarget).usePlaintext().build();
    try {

      // ##############################################################################
      // ## Assume we know the port here from the service node it is basically set through Gradle
      // here.
      // In your version you should first contact the registry to check which services
      // are available and what the port
      // etc is.

      /**
       * Your client should start off with 
       * 1. contacting the Registry to check for the available services
       * 2. List the services in the terminal and the client can
       *    choose one (preferably through numbering) 
       * 3. Based on what the client chooses
       *    the terminal should ask for input, eg. a new sentence, a sorting array or
       *    whatever the request needs 
       * 4. The request should be sent to one of the
       *    available services (client should call the registry again and ask for a
       *    Server providing the chosen service) should send the request to this service and
       *    return the response in a good way to the client
       * 
       * You should make sure your client does not crash in case the service node
       * crashes or went offline.
       */

      // Just doing some hard coded calls to the service node without using the
      // registry
      // create client
      Client client = new Client(channel, regChannel);

      // call the parrot service on the server
      //client.askServerToParrot(message);

      // ask the user for input how many jokes the user wants
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        //System.out.println("Services on the connected node. (without registry)");
        //client.getNodeServices(); // get all registered services


        while (true) {
          System.out.println("\nChoose a service:");
          System.out.println("1. Converter");
          System.out.println("2. Library");
          System.out.println("3. BucketList");
          System.out.println("4. Exit");

          String choice = reader.readLine();

          switch (choice) {
              case "1":
                  handleConverter(client, reader);
                  break;
              case "2":
                  handleLibrary(client, reader);
                  break;
              case "3":
                  handleBucketList(client, reader);
                  break;
              case "4":
                  return;
              default:
                  System.out.println("Invalid option");
          }
      }

      /// ///////////NOT USING JOKE OR ECHO//////////////////////////////////////

      // Reading data using readLine
      //System.out.println("How many jokes would you like?"); // NO ERROR handling of wrong input here.
      //String num = reader.readLine();

      // calling the joked service from the server with num from user input
      //client.askForJokes(Integer.valueOf(num));

      // adding a joke to the server
      //client.setJoke("I made a pencil with two erasers. It was pointless.");

      // showing 6 joked
      //client.askForJokes(Integer.valueOf(6));

      // list all the services that are implemented on the node that this client is connected to


      // ############### Contacting the registry just so you see how it can be done

      //if (args[5].equals("true")) {
        // Comment these last Service calls while in Activity 1 Task 1, they are not needed and wil throw issues without the Registry running
        // get thread's services
        //client.getServices(); // get all registered services

        // get parrot
        //client.findServer("services.Echo/parrot"); // get ONE server that provides the parrot service
        
        // get all setJoke
        //client.findServers("services.Joke/setJoke"); // get ALL servers that provide the setJoke service

        // get getJoke
        //client.findServer("services.Joke/getJoke"); // get ALL servers that provide the getJoke service

        // does not exist
        //client.findServer("random"); // shows the output if the server does not find a given service
      //}

    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent
      // leaking these
      // resources the channel should be shut down when it will no longer be used. If
      // it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      if (args[5].equals("true")) { 
        regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
    }
  }
    private static void handleConverter(Client client, BufferedReader reader) throws Exception {

        System.out.println("What do you want to convert?");
        System.out.println("1. Length");
        System.out.println("2. Weight");
        System.out.println("3. Temperature");

        String type = reader.readLine();

        switch (type) {
            case "1":
                System.out.println("Length units: KILOMETER, MILE, YARD, FOOT");
                break;
            case "2":
                System.out.println("Weight units: KILOGRAM, POUND");
                break;
            case "3":
                System.out.println("Temperature units: CELSIUS, FAHRENHEIT");
                break;
            default:
                System.out.println("Invalid option");
                return;
        }


        System.out.println("Enter value: ");
        double value;
        try {
            value = Double.parseDouble(reader.readLine());
        } catch (Exception e) {
            System.out.println("Invalid number");
            return;
        }


        System.out.println("From unit: ");
        String fromUnit = reader.readLine();

        System.out.println("To unit: ");
        String toUnit = reader.readLine();

        ConversionRequest request = ConversionRequest.newBuilder()
                .setValue(value)
                .setFromUnit(fromUnit)
                .setToUnit(toUnit)
                .build();

        ConversionResponse response = client.converterStub.convert(request);

        if(response.getIsSuccess()) {
            System.out.println("Result: " + response.getResult());
        } else {
            System.out.println("Error: " + response.getError());
        }


    }

    private static void handleLibrary(Client client, BufferedReader reader) throws Exception {
      System.out.println("Library options:");
      System.out.println("1. List books");
      System.out.println("2. Search books");
      System.out.println("3. Borrow book");
      System.out.println("4. Return book");

      String choice = reader.readLine();

      switch (choice) {
          case "1":
              BookListResponse response = client.libraryStub.listBooks(Empty.newBuilder().build());

              if (response.getIsSuccess()) {
                  for (Book book : response.getBooksList()) {
                      System.out.println(book.getTitle() + " written by " + book.getAuthor() +
                              (book.getIsBorrowed() ? "(Borrowed by " + book.getBorrowedBy() + ")" : ""));
                  }
              } else {
                  System.out.println(response.getError());
              }
              break;

          case "2":
              System.out.println("Enter title or author: ");
              String query = reader.readLine();
              BookSearchRequest request = BookSearchRequest.newBuilder()
                      .setQuery(query)
                      .build();

              BookListResponse resp = client.libraryStub.searchBooks(request);

              if (resp.getIsSuccess()) {
                  for (Book book : resp.getBooksList()) {
                      System.out.println(book.getTitle() + " written by " + book.getAuthor());
                  }
              } else {
                  System.out.println(resp.getError());
              }
              break;

          case "3":
              BookListResponse borrowResp = client.libraryStub.listBooks(Empty.newBuilder().build());

              if (borrowResp.getIsSuccess()) {
                  for (Book b : borrowResp.getBooksList()) {
                      System.out.println(
                              b.getTitle() + " by " + b.getAuthor() +
                                      " | ISBN: " + b.getIsbn()
                      );
                  }
              }
              System.out.println("Enter ISBN: ");
              String isbn = reader.readLine();

              if (isbn.isEmpty()) {
                  System.out.println("ISBN cannot be empty");
                  return;
              }

              System.out.println("Enter your name: ");
              String name = reader.readLine();

              BorrowRequest req = BorrowRequest.newBuilder()
                      .setIsbn(isbn)
                      .setBorrowerName(name)
                      .build();

              BorrowResponse res = client.libraryStub.borrowBook(req);
              if (res.getIsSuccess()) {
                  System.out.println(res.getMessage());
              } else {
                  System.out.println(res.getError());
              }
              break;

          case "4":
              BookListResponse borrowedList = client.libraryStub.listBooks(Empty.newBuilder().build());
              boolean foundBorrowed = false;

              if (borrowedList.getIsSuccess()) {
                  for (Book book : borrowedList.getBooksList()) {
                      if (book.getIsBorrowed()) {
                          foundBorrowed = true;

                          System.out.println(
                                  book.getTitle() + " by " + book.getAuthor() +
                                          " | ISBN: " + book.getIsbn() +
                                          " | Borrowed by: " + book.getBorrowedBy()
                          );
                      }

                  }
                  if (!foundBorrowed) {
                      System.out.println("No books are currently borrowed.");
                      return;
                  }
              }
              System.out.println("Enter ISBN: ");
                 String isbnReturn = reader.readLine();
              if (isbnReturn.isEmpty()) {
                  System.out.println("ISBN cannot be empty");
                  return;
              }

              ReturnRequest retRequest = ReturnRequest.newBuilder()
                      .setIsbn(isbnReturn)
                      .build();

              ReturnResponse retResp = client.libraryStub.returnBook(retRequest);

              if (retResp.getIsSuccess()) {
                  System.out.println(retResp.getMessage());
              } else {
                  System.out.println(retResp.getError());
              }
              break;
          default:
              System.out.println("Invalid option");

      }

    }
    private static void handleBucketList(Client client, BufferedReader reader) throws IOException {
        System.out.println("Bucket List Options:");
        System.out.println("1. Add item");
        System.out.println("2. List items");
        System.out.println("3. Complete item");
        System.out.println("4. Delete item");

        String choice = reader.readLine();

        switch (choice) {
            //add items to the bucket list
            case "1":
                System.out.println("Enter item description:");
                String desc = reader.readLine();

                AddItemRequest addReq = AddItemRequest.newBuilder()
                        .setDescription(desc)
                        .build();

                ItemResponse addRes = client.bucketStub.addItem(addReq);

                if (addRes.getIsSuccess()) {
                    System.out.println(addRes.getMessage());
                } else {
                    System.out.println(addRes.getError());
                }
                break;
            //list items in the bucket list
             case "2":
                 ItemListResponse listRes = client.bucketStub.listItems(Empty.newBuilder().build());

                 if (listRes.getIsSuccess()) {
                     List<Item> items = listRes.getItemsList();

                     for (int i = 0; i < items.size(); i++) {
                         Item item = items.get(i);

                         System.out.println(
                                 (i + 1) + ". " +
                                         item.getDescription() +
                                         (item.getIsCompleted() ? " (Completed)" : "")
                         );
                     }
                 } else {
                     System.out.println(listRes.getError());
                 }
                 break;

            //complete an item
            case "3":
                ItemListResponse compList = client.bucketStub.listItems(Empty.newBuilder().build());

                if (!compList.getIsSuccess()) {
                    System.out.println(compList.getError());
                    return;
                }

                List<Item> compItems = compList.getItemsList();

                for (int i = 0; i < compItems.size(); i++) {
                    Item item = compItems.get(i);
                    System.out.println((i + 1) + ". " + item.getDescription());
                }

                System.out.println("Select item number:");
                int compChoice = Integer.parseInt(reader.readLine());

                if (compChoice < 1 || compChoice > compItems.size()) {
                    System.out.println("Invalid selection");
                    return;
                }

                String compId = compItems.get(compChoice - 1).getId();

                ItemResponse compRes = client.bucketStub.completeItem(
                        ItemRequest.newBuilder().setId(compId).build()
                );

                System.out.println(
                        compRes.getIsSuccess() ? compRes.getMessage() : compRes.getError()
                );
                break;

                //delete an item

            case "4":
                ItemListResponse delList = client.bucketStub.listItems(Empty.newBuilder().build());

                if (!delList.getIsSuccess()) {
                    System.out.println(delList.getError());
                    return;
                }

                List<Item> delItems = delList.getItemsList();

                for (int i = 0; i < delItems.size(); i++) {
                    Item item = delItems.get(i);
                    System.out.println((i + 1) + ". " + item.getDescription());
                }

                System.out.println("Select item number:");
                int delChoice = Integer.parseInt(reader.readLine());

                if (delChoice < 1 || delChoice > delItems.size()) {
                    System.out.println("Invalid selection");
                    return;
                }

                String delId = delItems.get(delChoice - 1).getId();

                ItemResponse delRes = client.bucketStub.deleteItem(
                        ItemRequest.newBuilder().setId(delId).build()
                );

                System.out.println(
                        delRes.getIsSuccess() ? delRes.getMessage() : delRes.getError()
                );

                break;

            default:
                System.out.println("Invalid option");
        }
    }

}
