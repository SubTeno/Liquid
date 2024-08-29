package liquid.entity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

public class Messages extends ReactivePanacheMongoEntity {
    public ObjectId id;
    public ObjectId roomID;
    public String userID;
    public String text;
    public Date timestamp;

    public static List<Document> getMessagesbyRoom(String roomID) {
        return Arrays.asList(new Document("$match",
                new Document("roomID",
                        new ObjectId(roomID))),
                new Document("$lookup",
                        new Document("from", "User")
                                .append("localField", "userID")
                                .append("foreignField", "userID")
                                .append("as", "User")),
                new Document("$unwind", "$User"),
                new Document("$addFields",
                        new Document("userID", "$User")),
                new Document("$project",
                        new Document("User", 0L)));
    }
}