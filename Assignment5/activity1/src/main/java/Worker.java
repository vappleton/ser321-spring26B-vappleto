/**
 * Worker class that participates iin a distributed consensus system
 *
 * Each worker:
 * -Connects to the Leader using a socket
 * -Sends its name upon connection for identification
 * -Receives arithmetic tasks from the Leader
 * -Prompts the user to manually enter a result (no automatic calculation)
 * -Sends the result back to the Leader
 * -Receives and displays the consensus decision
 *
 * Workers operate independently and respond concurrently. They do not communicate with ecah other and rely on the Leader
 * for coordination.
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Worker {

    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.out.println("Please enter command in this format: gradle runWorker --args=<name> <host> <port>");
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

                if (message == null || message.equals("QUIT")) {
                    System.out.println("Leader disconnected.Shutting down");
                    break;
                }
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~TASK RECEIVED~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
                if (message.startsWith("TASK")) {
                    String task = message.substring(5); //rebuild "TASK"
                    System.out.println("Task received: " + task);

                    int userResult = 0;

                    while (true) {

                        System.out.println("> Enter your result: ");

                        if (in.ready()) { // check if Leader sent something new while waiting
                            String newMessage = in.readLine();

                            if (newMessage == null) {
                                System.out.println("Leader disconnected. Shutting down.");
                                return;
                            }
                            message = newMessage;
                            break; // exit input loop and process the new message
                        }
                        if (!scanner.hasNextLine()) {
                            System.out.println("Input closed.");
                            return;
                        }
                        String input = scanner.nextLine();

                        try {
                            userResult = Integer.parseInt(input);
                            break;
                        } catch (NumberFormatException e) {
                            System.out.println("Please enter a number!");
                        }

                    } // if interrupted, process the new message immediately
                    if (!message.startsWith("TASK")) {

                        if (message.equals("QUIT")) {
                            System.out.println("Leader disconnected. Shutting down.");
                            break;
                        } else if (message.startsWith("CONSENSUS")) { //consesus received

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
                            continue;
                        }
                        continue;
                    }

                    //send the result
                    out.println("RESULT " + userResult);
                    lastVote = userResult;

                    System.out.println("Result submitted to leader.\n");

                }

                //~~~~~~~~~~~~~~~~~~~~~~~~~~~CONSENSUS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
                else if (message.startsWith("CONSENSUS")) { //consesus received

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

                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ERROR CONSENSUS~~~~~~~~~~~~~~~~~~~~~~~//
                else if (message.startsWith("ERR-CONS")) {
                    System.out.println("Consensus failed: " + message);
                    System.out.println("Waiting for next task...\n");
                }
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("Connection lost. Exiting!");
        }

    }
}
