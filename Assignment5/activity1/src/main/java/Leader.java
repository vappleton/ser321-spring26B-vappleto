/**
 * Leader class that coordinates a distributed consensus system using a hierarchical Leader-Worker model
 * The leader:
 * -Accepts connections from multiple workers
 * -Sends arithmetic tasks to all connected workers
 * -Collects results concurrently using threads
 * -Uses majority voting to determine consensus
 * -Handles worker failures and timeouts
 *
 * The system requires a minimum of 3 workers and allows up to 5 workers to participate in each consensus round.
 *
 * Consensus is based on agreement and not correctness. The Leader does not verify the result, so incorrect consensus is possible if
 * the majority of the workers provide the same incorrect value.
 */

import java.io.*;
import java.net.*;
import java.util.*;



public class Leader {

    private static List<Socket> workers = new ArrayList<>();
    private static Map<Socket, BufferedReader> inputs = new HashMap<>();
    private static Map<Socket, PrintWriter> outputs = new HashMap<>();
    private static Map<Socket, String> workerNames = new HashMap<>();
    private static volatile boolean running = true;


    public static void main(String[] args) throws IOException, InterruptedException {


        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Leader starting on port " + port + "\n");

        System.out.println("Waiting for workers to connect... (need at least 3)");

        //accept workers in separate threads (handles incomming connections)
        new Thread(() -> {
            while (running) {
                try {
                    Socket workerSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(workerSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(workerSocket.getOutputStream(), true);

                    String workerName = in.readLine(); //having the worker send name first

                    synchronized (workers) {
                        workers.add(workerSocket);
                        inputs.put(workerSocket, in);
                        outputs.put(workerSocket, out);
                        workerNames.put(workerSocket, workerName);
                    }
                    System.out.println(workerName + " connected: from " + workerSocket.getInetAddress().getHostName()
                            + ":" + workerSocket.getPort());

                } catch (IOException e) {
                    if (running) {
                        System.out.println("Error accepting worker");
                    }
                }
            }
        }).start();

        while (true) { //wait for at least 3 workers

            synchronized (workers) {
                if (workers.size() >= 3) break;
            }
            Thread.sleep(200);
        }
        //wait for up to 5 workers or timeout
        long waitStart = System.currentTimeMillis();
        long waitLimit = 5000; // 5 seconds

        while (true) {
            synchronized (workers) {
                if (workers.size() >= 5) break;
            }

            if (System.currentTimeMillis() - waitStart >= waitLimit) break;

            Thread.sleep(200);
        }

        System.out.println("All " + workers.size() + " workers connected. Starting consensus rounds...");

        Scanner scanner = new Scanner(System.in);

        //rounds
        int round = 1;
        while (true) {
            if (round == 1) {
                System.out.println("Please enter an arithmetic task:");
            } else {
                System.out.println("\nPlease enter an arithmetic task (or 'quit'):");
            }
            String input = scanner.nextLine();

            //input validation
            if (!input.equalsIgnoreCase("quit") &&
                    !input.matches("\\d+\\s*[+\\-*/]\\s*\\d+")) {
                System.out.println("Invalid task format. Use something like 2+2 or 10 - 3");
                continue;
            }
            //quit
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");

                synchronized (workers) {
                    for (Socket s : workers) {
                        try {
                            outputs.get(s).println("QUIT");
                            outputs.get(s).flush();
                            s.close();
                        } catch (IOException ignored) {}
                    }
                }
                running = false;
                serverSocket.close();
                break;
            }

            int retries =0;
            boolean consensusReached = false;

            while (!consensusReached && retries < 2) {

                System.out.println("\nRound " + round + ": Assigning Task \"" + input + "\"");

                String task = "TASK " + input;

                //now sending the task to all the workrs

                List<Socket> activeWorkers;
                synchronized (workers) {
                    activeWorkers = new ArrayList<>(workers);
                }

                //send the task
                for (Socket s : activeWorkers) {
                    PrintWriter out = outputs.get(s);

                    if (out != null) {
                        out.println(task);
                    }
                }

                Map<Integer, Integer> votes = new HashMap<>();

                //create a listener
                WorkerListener listener = new WorkerListener(activeWorkers.size());

                //collect responses concurrently
                for (Socket s : activeWorkers) {
                    new Thread(() -> {
                        try {
                            String response = inputs.get(s).readLine();

                            if (response == null) {
                                System.out.println("Worker " + workerNames.get(s) + " was disconnected.");

                                synchronized (workers) {
                                    workers.remove(s);
                                    inputs.remove(s);
                                    outputs.remove(s);
                                    workerNames.remove(s);
                                }
                                try { s.close(); } catch (IOException ignored) {}

                            } else if (response.startsWith("RESULT")) {
                                int value = Integer.parseInt(response.split(" ")[1]);

                                synchronized (votes) {
                                    votes.put(value, votes.getOrDefault(value, 0) + 1);
                                }
                                System.out.println("Received from " + workerNames.get(s) + ": " + value);
                            }

                        } catch (IOException e) {
                            System.out.println("Worker " + workerNames.get(s) + " disconnected");

                            synchronized (workers) {
                                workers.remove(s);
                                inputs.remove(s);
                                outputs.remove(s);
                                workerNames.remove(s);
                            }

                        } catch (Exception e) {
                            System.out.println("Unexpected error with worker " + workerNames.get(s));
                        } finally {
                            listener.workerDone();
                        }
                    }).start();

                }

                //Now waiting for all responses
                listener.waitForAll(15000);

                //process results
                int total = 0;
                for (int count : votes.values()) {
                    total += count;
                }
                if (total == 0) {
                    System.out.println("No responses received.");
                    retries++;

                    if (retries < 2) {
                        System.out.println("Retrying the same task...");
                        continue;

                    }else {
                        System.out.println("No responses after retry. Moving to the next task.");
                        round++;
                        break;
                    }
                }

                int max_votes = 0;
                for (int count : votes.values()) {
                    if (count > max_votes) {
                        max_votes = count;
                    }
                }

                List<Integer> candidates = new ArrayList<>();
                for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
                    if (entry.getValue() == max_votes) {
                        candidates.add(entry.getKey());
                    }
                }

                if (candidates.size() == 1 && max_votes > total /2) {

                    int consensus = candidates.get(0);


                    System.out.println("Consensus: " + consensus + " (" + max_votes + "/" + total + " workers agreed)");
                    System.out.println("Announcing consensus to all workers...");

                    String message = "CONSENSUS " + consensus + " " + max_votes + "/" + total;

                    for (Socket s : activeWorkers) {
                        PrintWriter out = outputs.get(s);

                        if (out != null) {
                            out.println(message);
                        }
                    }

                    consensusReached = true;
                    round++;

                } else if (candidates.size() > 1) {
                    retries++;
                    if (retries < 2) {
                        System.out.println("There's a tie!. Votes: " + votes + " Retrying the same task...");
                    } else {
                        System.out.println("The tie persisted. No consensus was reached. Moving to next task.");

                        String message = "ERR-CONS Tie persisted";

                        for (Socket s : activeWorkers) {
                            PrintWriter out = outputs.get(s);
                            if (out != null) {
                                out.println(message);
                            }
                        }

                        round++;
                        break;
                    }
                } else {
                    System.out.println("No consensus reached. Votes: " + votes);
                    String message = "ERR-CONS No consensus";

                    for (Socket s : activeWorkers) {
                        PrintWriter out = outputs.get(s);
                        if (out != null) {
                            out.println(message);
                        }
                    }

                    round++;
                    break;

                }
            }

        }

        System.out.println("Leader shutting down.");
        System.exit(0);
    }



}
