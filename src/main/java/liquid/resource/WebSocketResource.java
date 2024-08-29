package liquid.resource;

import java.util.Date;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.oidc.UserInfo;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import liquid.entity.Messages;
import liquid.entity.Room;
import liquid.entity.User;
import liquid.util.KeycloakService;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.mutiny.redis.client.RedisAPI;

@WebSocket(path = "room/{roomID}/ws")
@SessionScoped
public class WebSocketResource {

    @Inject
    TenantIdentityProvider idp;

    @Inject
    ReactiveRedisDataSource redisAPI;

    @Inject
    RedisAPI blockredis;

    @Inject
    JsonWebToken jwt;

    @Inject
    JWTParser jwtParser;

    @Inject
    KeycloakService kc;

    @Inject
    UserInfo user;

    @Inject
    ObjectMapper om;

    private String roomID;

    private Cancellable sub;

    @OnOpen
    public void open(HandshakeRequest req, WebSocketConnection connection) throws Exception {
        roomID = connection.pathParam("roomID");
        authorize(req, connection);
        sub = redisAPI.pubsub(String.class).subscribe(roomID).subscribe().with(body -> {
            connection.sendText(body);
        });
    }

    @OnTextMessage
    void OnTextMessage(String body, WebSocketConnection connection) throws Exception {

        ResponseJSON jMessage = new ResponseJSON();
        ResponseJSON notification = new ResponseJSON();

        Integer userCredit = Integer.parseInt(kc.getUserAttribute(jwt.getSubject(), "credit"));

        Uni<User> user = User.find("userID", jwt.getSubject()).firstResult();
        String userNickname = user.await().indefinitely().nickname;
        //
        if (userCredit <= 2500) {
            jMessage.type = "ERROR";
            jMessage.body = "NOT ENOUGH CREDIT";
            connection.sendText(jMessage).subscribeAsCompletionStage();
            return;
        }
        Messages msg = new Messages();
        msg.roomID = new ObjectId(roomID);
        msg.timestamp = new Date();
        msg.text = body;
        msg.userID = jwt.getSubject();

        try {

            msg.persist().await().indefinitely();

            kc.addCredit(jwt.getSubject(), -2500);

            jMessage.type = "MESSAGE";
            jMessage.sender = userNickname;
            jMessage.body = body;
            jMessage.credDeduct = 2500;
            notification.type = "NOTIFICATION";
            notification.body = "OK";
            connection.sendTextAndAwait(om.writeValueAsString(notification));
            redisAPI.pubsub(String.class).publish(roomID, om.writeValueAsString(jMessage))
                    .subscribeAsCompletionStage();


        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @OnClose
    void OnClose() {
        sub.cancel();
    }

    private void authorize(HandshakeRequest req, WebSocketConnection connection) throws Exception {
        final String token = req.header("Sec-WebSocket-Protocol").replace("Authorization, ", "");
        idp.authenticate(new AccessTokenCredential(token))
                .await()
                .indefinitely();
        jwt = jwtParser.parseOnly(token);

        Room rm = Room.getRoom(roomID);
        if (rm == null) {
            connection.close();
        }

        return;
    }

    private static class ResponseJSON {
        @SuppressWarnings("unused")
        public String type;
        @SuppressWarnings("unused")
        public String body;
        @SuppressWarnings("unused")
        public Integer credDeduct;
        @SuppressWarnings("unused")
        public String sender;
    }
}
