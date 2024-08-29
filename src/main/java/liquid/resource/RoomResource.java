package liquid.resource;

import org.jboss.resteasy.reactive.RestPath;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.TemplateInstance;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import liquid.resource.WebResource.Templates;
import liquid.util.KeycloakService;

@Path("/room")

public class RoomResource extends HxController {

    

    @Inject
    KeycloakService kc;

    @Inject
    RedisAPI redisAPI;

    

    @GET
    @Path("/{roomid}")
    public TemplateInstance getMessages(@RestPath String roomid) throws JsonProcessingException {

        if (isHxRequest()) {
            return Templates.index$chat();
        }
        
        return Templates.index();
    }

    

}
