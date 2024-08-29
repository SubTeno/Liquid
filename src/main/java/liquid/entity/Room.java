package liquid.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Room extends ReactivePanacheMongoEntity {

    public ObjectId id;
    public Date start_time;
    public List<UserRoom> participants;

    public static Room getRoom(String roomid) {
        if (!ObjectId.isValid(roomid)) {
            return null;
        }
        ObjectId oid = new ObjectId(roomid);
        Uni<Room> rm = Room.findById(oid);
        try {
            Room res = rm.await().indefinitely();
            return res;

        } catch (Exception e) {
            System.out.println(e);
            return null;
        }

    }

    public static List<Document> findRoom(String uID) {
        return Arrays.asList(new Document("$match",
                new Document("participants.userID", uID)),
                new Document("$lookup",
                        new Document("from", "User")
                                .append("localField", "participants.userID")
                                .append("foreignField", "userID")
                                .append("as", "User")),
                new Document("$addFields",
                        new Document("participants",
                                new Document("$map",
                                        new Document("input", "$participants")
                                                .append("as", "p")
                                                .append("in",
                                                        new Document("$mergeObjects", Arrays.asList("$$p",
                                                                new Document("userID",
                                                                        new Document("$arrayElemAt", Arrays.asList(
                                                                                "$User",
                                                                                new Document("$indexOfArray",
                                                                                        Arrays.asList("$User.userID",
                                                                                                "$$p.userID"))))))))))),
                new Document("$project",
                        new Document("User", 0L)));
    }

}
