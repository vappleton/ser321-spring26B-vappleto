/**
 * BucketListImpl implements the BucketList gRPC service.
 *
 * This service allows clients to:
 * - Add new bucket list items
 * - List all items
 * - Mark items as completed
 * - Delete items
 *
 * Each item is stored with:
 * - A unique ID (UUID)
 * - A description
 * - A completion status
 *
 * Persistence:
 * - All data is stored in a JSON file ("bucket_data.json")
 * - Data is loaded when the server starts
 * - Data is saved after every modification (add, complete, delete)
 *
 * Design Notes:
 * - Uses a HashMap for fast lookup by ID
 * - Uses UUIDs internally for uniqueness
 * - Client displays items using user-friendly indexing instead of UUIDs
 *
 * This implementation satisfies the assignment requirements by supporting
 * multiple RPC methods, handling input validation, returning different response types,
 * using repeated fields, and maintaining persistent server-side data.
 */


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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BucketListImpl extends BucketListGrpc.BucketListImplBase{

    class BucketItem {
        String id;
        String description;
        boolean isCompleted;

        BucketItem(String id, String description, boolean isCompleted) {
            this.id = id;
            this.description = description;
            this.isCompleted = isCompleted;
        }
        //newBucket item constructor
        BucketItem(String id, String description) {
            this.id = id;
            this.description = description;
            this.isCompleted = false;
        }
    }


    public BucketListImpl() {
        loadData();
    }

    private void loadData() {
        try {
            File file = new File(FILE_NAME);

            if (!file.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            reader.close();

            JSONArray jsonArray = new JSONArray(sb.toString());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                BucketItem item = new BucketItem(
                        obj.getString("id"),
                        obj.getString("description"),
                        obj.getBoolean("isCompleted")
                );

                items.put(item.id, item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Map<String, BucketItem> items = new HashMap<>();
    private final String FILE_NAME = "bucket_data.json";

    @Override
    public void addItem(AddItemRequest request, StreamObserver<ItemResponse> responseObserver) {
        String description = request.getDescription();

        if (description == null || description.isEmpty()) {
            responseObserver.onNext(
                    ItemResponse.newBuilder()
                            .setIsSuccess(false)
                            .setError("missing field")
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        String id = UUID.randomUUID().toString(); //creating a unique id for each bucket list item

        BucketItem item = new BucketItem(id, description);
        items.put(id, item);

        saveData();

        responseObserver.onNext(
                ItemResponse.newBuilder()
                        .setIsSuccess(true)
                        .setMessage("Item added successfully")
                        .build()
        );

        responseObserver.onCompleted();
    }

    @Override
    public void listItems(Empty request, StreamObserver<ItemListResponse> responseObserver) {
        if (items.isEmpty()) {
            responseObserver.onNext(
                    ItemListResponse.newBuilder()
                            .setIsSuccess(false)
                            .setError("no items found")
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        ItemListResponse.Builder response = ItemListResponse.newBuilder()
                .setIsSuccess(true);

        for (BucketItem item : items.values()) {
            response.addItems(
                    Item.newBuilder()
                            .setId(item.id)
                            .setDescription(item.description)
                            .setIsCompleted(item.isCompleted)
                            .build()
            );
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void completeItem(ItemRequest request, StreamObserver<ItemResponse> responseObserver) {
        String id = request.getId();

        if (!items.containsKey(id)) {
            responseObserver.onNext(
                    ItemResponse.newBuilder()
                            .setIsSuccess(false)
                            .setError("item not found")
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        BucketItem item = items.get(id);

        if (item.isCompleted) {
            responseObserver.onNext(
                    ItemResponse.newBuilder()
                            .setIsSuccess(false)
                            .setError("item already completed")
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        item.isCompleted = true;
        saveData();

        responseObserver.onNext(
                ItemResponse.newBuilder()
                        .setIsSuccess(true)
                        .setMessage("Item marked as completed")
                        .build()
        );

        responseObserver.onCompleted();
    }

    @Override
    public void deleteItem(ItemRequest request, StreamObserver<ItemResponse> responseObserver) {
        String id = request.getId();

        if (!items.containsKey(id)) {
            responseObserver.onNext(
                    ItemResponse.newBuilder()
                            .setIsSuccess(false)
                            .setError("item not found")
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        items.remove(id);
        saveData();

        responseObserver.onNext(
                ItemResponse.newBuilder()
                        .setIsSuccess(true)
                        .setMessage("Item deleted")
                        .build()
        );

        responseObserver.onCompleted();
    }

    //helper to ensure persistence
    private void saveData() {
        try {
            JSONArray jsonArray = new JSONArray();

            for (BucketItem item : items.values()) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("description", item.description);
                obj.put("isCompleted", item.isCompleted);

                jsonArray.put(obj);
            }

            FileWriter writer = new FileWriter(FILE_NAME);
            writer.write(jsonArray.toString());
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
