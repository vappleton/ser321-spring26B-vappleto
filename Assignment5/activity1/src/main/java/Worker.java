import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Worker {

    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.out.println("Please enter command in this format: Worker <name> <host> <port>");
            return;
        }

        String workerName = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        try {
            System.out.println(workerName + " Connecting to leader at " + host + ":" + port);

            Socket socket = new Socket(host, port);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            //send the worker name first
            out.println(workerName);

            System.out.println("Connected successfully!\n");

            Scanner scanner = new Scanner(System.in);

            int lastVote = Integer.MIN_VALUE;

            while (true) {
                String message = in.readLine();

                if (message == null) {
                    System.out.println("Leader disconnected.");
                    break;
                }
                //task was received
                if (message.startsWith("TASK")) {
                    String task = message.substring(5); //remove "TASK"
                    System.out.println("Task received: " + task);

                    System.out.println("> Enter your result: ");

                    int userResult = Integer.parseInt(scanner.nextLine());

                    lastVote = userResult;
                    out.println("RESULT " + userResult);

                    System.out.println("Result submitted to leader.\n");

                } else if (message.startsWith("CONSENSUS")){ //consesus received

                    String[] parts = message.split(" ");

                    int consensus = Integer.parseInt(parts[1]);

                    String ratio = parts[2];

                    System.out.println("Consensus announced: " + consensus + " (" + ratio + " workers agreed)");

                    if (consensus == lastVote) {
                        System.out.println("You voted with the majority!\n");
                    } else {
                        System.out.println("Your answer differed from the consensus.\n");
                    }

                    System.out.println("Waiting for next task...\n");
                }

                // ERROR CONSENSUS
                else if (message.startsWith("ERR-CONS")) {
                    System.out.println("Consensus failed: " + message);
                    System.out.println("Waiting for next task...\n");
                }

                // QUIT
                else if (message.equals("QUIT")) {
                    System.out.println("Shutting down.");
                    break;
                }

            }
            socket.close();
        } catch (IOException e) {
            System.out.println("Connection lost. Exiting!");
        }

    }
}
