package liquid.util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import liquid.entity.User;

import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.RedisAPI;

/**
 * EventConsumer
 */
@ApplicationScoped
public class EventConsumer {

    @Inject
    KeycloakService kc;

    @Inject
    RedisAPI redisAPI;

    @ActivateRequestContext
    @Incoming("messages")
    public void consume(JsonObject message) {
        JsonObject details = message.getJsonObject("details");
        String userID = message.getString("userId");
        switch (message.getString("type")) {
            case "REGISTER":
                register(userID);
                break;
            case "UPDATE_PROFILE":
                Uni<User> Uniuser = User.find("userID", userID).firstResult();
                User Updateduser = Uniuser.await().indefinitely();
                if (Updateduser == null) {
                    register(userID);
                    break;
                }
                Updateduser.nickname = details.getString("updated_nickname");
                Updateduser.update().await().indefinitely();
                break;

            default:
                break;
        }

        return;

    }

    private void register(String userID) {
        User user = new User();
        String nickname = kc.getUserResource(userID.toString()).toRepresentation().firstAttribute("nickname");
        user.userID = userID;
        user.nickname = nickname;
        user.persist().await().indefinitely();
    }
}