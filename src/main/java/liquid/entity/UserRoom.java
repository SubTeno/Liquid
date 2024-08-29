package liquid.entity;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

public class UserRoom extends ReactivePanacheMongoEntity {
    public String idkeys;

    public String userID;
   
    public String otkeys;
}
