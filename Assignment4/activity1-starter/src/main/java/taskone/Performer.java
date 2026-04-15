package taskone;

import java.io.*;
import java.net.Socket;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;
import taskone.proto.Request;
import taskone.proto.Response;
import taskone.proto.TaskProto;

/**
 * Performer class handles client requests using JSON protocol.
 * This version uses JSON for serialization.
 */
public class Performer {
    private final Socket clientSocket;
    private final TaskList taskList;

    private InputStream inStream; // For proto
    private OutputStream outStream; // For proto

    private BufferedReader in; // For JSON
    private PrintWriter out; // For JSON

    public Performer(Socket clientSocket, TaskList taskList) {
        this.clientSocket = clientSocket;
        this.taskList = taskList;
    }

    /**
     * Main method to process client requests.
     * Reads requests, processes them, and sends responses.
     */
    public void doPerform() {
        try {
            inStream = clientSocket.getInputStream();
            outStream = clientSocket.getOutputStream();
            in = new BufferedReader(new InputStreamReader(inStream));
            out = new PrintWriter(outStream, true);

            // Use either JSON or Proto from the examples below,
            // and make matching changes in Client.java.

                /////////////////////////////////////////////////////////////////////////////
                            // Welcome JSON
                /////////////////////////////////////////////////////////////////////////////
            // Send welcome message. You can keep this as JSON.
            JSONObject welcomeMessage = JsonUtils.createSuccessResponse("connect", "Connected to Task Management Server");
            out.println(welcomeMessage);
                /////////////////////////////////////////////////////////////////////////////
                            // End Welcome JSON
                /////////////////////////////////////////////////////////////////////////////



                /////////////////////////////////////////////////////////////////////////////
                            // Welcome Proto
                /////////////////////////////////////////////////////////////////////////////
//            Response.Builder protoResp = Response.newBuilder().setType(Response.ResponseType.SUCCESS).setMessage("Connected to Proto Task Management Server");
//            protoResp.build().writeDelimitedTo(outStream);
                /////////////////////////////////////////////////////////////////////////////
                            // End Welcome Proto
                /////////////////////////////////////////////////////////////////////////////




            // Process requests

            while (true) {
                Request request = Request.parseDelimitedFrom(inStream); // changed to proto
                //request = in.readLine(); // Read as String JSON, not Proto yet.
                // We intentionally skip error handling here to focus on Proto conversion.
                // This may fail if the request is malformed or missing expected fields, which is ok this time.

                // Once you start changing things more and more to proto, the JSON parts might not work anymore.
                // That is fine, just do not call these requests until converted.
                // Start with the "add" request.

                System.out.println(request);
                Request.RequestType type = request.getType();
                Response response;

                //JSONObject responseJSON;
                System.out.println(type);
                // Change all the following requests/responses to JSON here and in Client.java

                switch (type) {
                    case ADD:
                        response = handleAdd(request); // would need to be changed to return ProtoRes and get ProtReq
                        break;
                    case LIST:
                       response = handleList(request);
                        break;
                    case FINISH:
                        response = handleFinish(request);
                        break;
                        case QUIT:
                        response = handleQuit();
                        break;
                    default:
                        response = Response.newBuilder().setType(Response.ResponseType.ERROR)
                                .setMessage("Unknown request type: ").build();
                }

                //out.println(responseJSON.toString()); // send json
                response.writeDelimitedTo(outStream); //send proto
                outStream.flush();

                // If quit, break the loop
                    if (type == Request.RequestType.QUIT) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private Response handleAdd(Request request) { // will need to change to not use JSON anymore - or make new method
        // Validation is intentionally removed so students can focus on Proto conversion.
        // These comments show what production-style validation would look like. You can also delete all these
        // if they annoy you since it makes it harder to read it

//        // Validate required fields
//        if (!request.has("description")) {
//            return JsonUtils.createErrorResponse("add", "Missing 'description' field");
//        }
//        if (!request.has("category")) {
//            return JsonUtils.createErrorResponse("add", "Missing 'category' field");
//        }

        String description = request.getDescription();
        String category = request.getCategory();

//        // Validate description not empty
//        if (description.trim().isEmpty()) {
//            return JsonUtils.createErrorResponse("add", "Description cannot be empty");
//        }
//
//        // Validate category value
//        if (!category.equals("work") && !category.equals("personal") && !category.equals("school") && !category.equals("other")) {
//            return JsonUtils.createErrorResponse("add", "Invalid category value. Must be 'work', 'personal', 'school', or 'other'");
//        }

        // Add task
        Task task = taskList.addTask(description, category); // Assume valid input for this starter version.
        TaskProto protoTask = TaskProto.newBuilder().setId(task.getId()).setDescription(task.getDescription())
                .setCategory(task.getCategory()). setAssignee(task.getAssignee())
                .setFinished(task.isFinished()).build();

        // Return success response with created task
        return Response.newBuilder().setType(Response.ResponseType.SUCCESS)
                .setMessage("Task added").setTask(protoTask).build();
    }


    private Response handleList(Request request) {
        // Get filter (defaults to "all")
        String filter = request.getFilter().isEmpty() ? "all" : request.getFilter();

        List<Task> tasks;
        switch (filter) {
            case "all":
                tasks = taskList.listAllTasks();
                break;
            case "pending":
                tasks = taskList.listPendingTasks();
                break;
            case "finished":
                tasks = taskList.listFinishedTasks();
                break;
            default:
                return Response.newBuilder()
                        .setType(Response.ResponseType.ERROR)
                        .setMessage("Invalid filter value. Must be 'all', 'pending', or 'finished'")
                        .build(); // Keep similar error semantics in Proto.
        }

        Response.Builder responseBuilder = Response.newBuilder()
                .setType(Response.ResponseType.SUCCESS)
                .setMessage("Tasks listed");


        // add tasks to the list
        for (Task task : tasks) {
            TaskProto protoTask = TaskProto.newBuilder()
                    .setId(task.getId())
                    .setDescription(task.getDescription())
                    .setCategory(task.getCategory())
                    .setAssignee(task.getAssignee())
                    .setFinished(task.isFinished())
                    .build();
            responseBuilder.addTasks(protoTask);
        }
        responseBuilder.setCount(tasks.size());


        return responseBuilder.build();
    }

    private Response handleFinish(Request request) {
        // Validation intentionally skipped.

        int id = request.getId();
        // Mark task as finished
        boolean success = taskList.finishTask(id);

        if (success) {
            return Response.newBuilder()
                    .setType(Response.ResponseType.SUCCESS)
                    .setMessage("Task # " + id + " has been finished.").build();
        } else {
            return Response.newBuilder()
                    .setType(Response.ResponseType.ERROR)
                    .setMessage("Task not found with ID: " + id)
                    .build();
        }
    }

    private Response handleQuit() {
        return Response.newBuilder()
                .setType(Response.ResponseType.SUCCESS)
                .setMessage("Goodbye!")
                .build();
    }
}
