
import java.io.*;
import java.net.*;
import java.util.*;



public class Leader {

    private static List<Socket> workers = new ArrayList<>();
    private static Map<Socket, BufferedReader> inputs = new HashMap<>();
    private static Map<Socket, PrintWriter> outputs = new HashMap<>();
    private static Map<Socket, String> workerNames = new HashMap<>();


    public static void main(String[] args) throws IOException, InterruptedException {


        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Leader starting on port " + port + "\n");

        System.out.println("Waiting for workers to connect... (need at least 3)");

        //accept workers in separate threads

        new Thread(() -> {
            while (true) {
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
                    System.out.println("Error accepting worker");
                }
            }
        }).start();

        while (true) { //wait for at least 3 workers but no more than 5 (following the given sample output)

            synchronized (workers) {
                if (workers.size() >= 5) break;
            }
            Thread.sleep(1000);
        }
        System.out.println("All " + workers.size() + " workers connected. Starting consensus rounds...");

        Scanner scanner = new Scanner(System.in);
        //rounds
        int round = 1;
        while (true) {
            System.out.println("Please enter an arithmetic task: \n");
            String input = scanner.nextLine();

            if (input.equals("quit")) {
                System.out.println("Goodbye!");
                break;
            }
            System.out.println("\nRound " + round + ": Assigning Task \"" + input + "\"");

            String task = "TASK " + input;

            //now sending the task to all the workrs

            synchronized (workers) {
                for (Socket s : workers) {
                    outputs.get(s).println(task);
                }
            }

            Map<Integer, Integer> votes = new HashMap<>();

            //receiving results (each worker handlded in its own thread)

            synchronized (workers) {
                for (Socket s : workers) {
                    new Thread(() -> {
                        try {
                            s.setSoTimeout(15000); //tmeout

                            String response = inputs.get(s).readLine();

                            if (response != null && response.startsWith("RESULT")) {
                                int value = Integer.parseInt(response.split(" ")[1]);

                                synchronized (votes) {
                                    votes.put(value, votes.getOrDefault(value, 0) + 1);
                                }
                                System.out.println("Received from " + workerNames.get(s) + ": " + value);
                            }

                        } catch (Exception e) {
                            System.out.println("Worker + " + workerNames.get(s) + " timed out or failed.");
                        }
                    }).start();

                }
            }

            //Now waiting for responses
            Thread.sleep(15000);

            int total =0;
            for (int count : votes.values()) {
                total += count;
            }
            if (total == 0) {
                System.out.println("No responses received.");
                continue;
            }
            int max_votes =0;
            for (int count : votes.values()) {
                if (count > max_votes) {
                    max_votes = count;
                }
            }
            if(max_votes >= Math.ceil(total *0.5)) {
                //finding the candidate wuth max votes

                List<Integer> candidates = new ArrayList<>();
                for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
                    if (entry.getValue() == max_votes) {
                        candidates.add(entry.getKey());
                    }
                }

                int consensus;

                if (candidates.size() == 1) {
                    consensus = candidates.get(0);
                } else {
                    //tie-breaking: choose the smallest value
                    consensus = Collections.min(candidates);
                }

                System.out.println("Consensus: " + consensus + " " + max_votes + "/" + total);
                System.out.println("Announcing consensus to all workers...");


            } else {
                System.out.println("No consensus reached. Votes: " + votes);
            }
            round++;

        }
        System.out.println("Leader shutting down.");

    }



}
