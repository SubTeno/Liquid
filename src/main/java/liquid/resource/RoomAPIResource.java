package liquid.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import liquid.entity.Messages;
import liquid.entity.Room;
import liquid.entity.UserRoom;

@RequestScoped
@Authenticated
@Path("/api/v1/room")
public class RoomAPIResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    MongoClient mongoClient;

    @Inject
    ObjectMapper om;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getRoom() throws JsonProcessingException {
        List<Document> result = mongoClient.getDatabase("liquid").getCollection("Room")
                .aggregate(Room.findRoom(jwt.getSubject())).into(new ArrayList<>());
        return om.writeValueAsString(result);
    }

    @GET
    @Path("/{roomid}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMessages(@RestPath String roomid) throws JsonProcessingException {
        List<Document> result = mongoClient.getDatabase("liquid").getCollection("Messages")
                .aggregate(Messages.getMessagesbyRoom(roomid))
                .into(new ArrayList<>());
        return om.writeValueAsString(result);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{roomid}/join")
    public Response joinRoom(@RestPath String roomid, ResponseJSON json) {
        if (json.idkeys == null || json.otkeys == null) {
            return Response.status(Status.ACCEPTED).build();
        }
        Room rm = Room.getRoom(roomid);
        if (rm == null || rm.participants.stream().filter(e -> e.userID.equals(jwt.getSubject())).count() == 1) {
            return Response.status(StatusCode.NOT_ACCEPTABLE).build();
        }
        try {
            UserRoom ur = new UserRoom();
            ur.userID = jwt.getSubject();
            ur.idkeys = json.idkeys;
            ur.otkeys = json.otkeys;
            rm.participants.add(ur);
            rm.update().await().indefinitely();
        } catch (Exception e) {
            System.out.println(e);
        }
        return Response.ok().build();
    }

    @POST
    @Path("{roomid}/leave")
    public Response leaveRoom(@RestPath String roomid) {
        Room rm = Room.getRoom(roomid);
        Optional<UserRoom> ur = rm.participants.stream().filter(e -> e.userID.equals(jwt.getSubject())).findFirst();

        if (ur.isEmpty()) {
            return Response.status(Status.EXPECTATION_FAILED).build();
        }
        if (rm.participants.size() == 1) {
            Room.deleteById(new ObjectId(roomid)).await().indefinitely();
            return Response.ok().build();

        } else {
            rm.participants.remove(ur.get());
        }

        rm.persistOrUpdate().await().indefinitely();
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/register")
    public Response registerRoom(ResponseJSON json) {

        if (json.idkeys == null || json.otkeys == null) {
            return Response.status(Status.NOT_ACCEPTABLE).build();
        }
        final Room rm = new Room();

        UserRoom ur = new UserRoom();
        ur.idkeys = json.idkeys;
        ur.otkeys = json.otkeys;
        ur.userID = jwt.getSubject();

        rm.participants = List.of(ur);
        rm.start_time = new Date();
        try {
            rm.persist().await().indefinitely();
            return Response.ok(rm.id).build();

        } catch (Exception e) {
            System.out.println(e);

            return Response.status(Status.NOT_ACCEPTABLE).build();
        }
    }

    private static class ResponseJSON {
        public String idkeys;
        public String otkeys;

    }
}
