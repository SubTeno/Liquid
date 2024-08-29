package liquid.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import liquid.util.KeycloakService;

import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.mutiny.redis.client.RedisAPI;

@Path("/")
public class WebResource extends HxController {

    @Inject
    JsonWebToken jwt;

    @Inject
    RedisAPI redisAPI;

    @Inject
    KeycloakService kc;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index();

        public static native TemplateInstance index$chat();

        public static native TemplateInstance index$rooms();
    }

    @Path("/")
    public TemplateInstance index() {
        
        return Templates.index();
    }

    @Path("/topup")
    @POST
    public Response topup(Integer credit){
        try {
            kc.addCredit(jwt.getSubject(), credit);
            return Response.ok().build();
        } catch (Exception e) {
            System.out.println(e);
            return Response.status(Status.NOT_ACCEPTABLE).build();
        }
        
    }

}
