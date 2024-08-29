package liquid.entity;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

public class User extends ReactivePanacheMongoEntity {
    public ObjectId id;
    public String userID;
    public String nickname;
}
