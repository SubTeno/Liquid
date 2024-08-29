package liquid.util;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KeycloakService {

    @Inject
    Keycloak keycloak;

    private UserRepresentation userRepresentation;
    private UserResource userResource;

    public void addCredit(String userID, Integer credit) {
        try {
            Integer userCredit = Integer.parseInt(getUserAttribute(userID, "credit"));
            userRepresentation.singleAttribute("credit", String.valueOf(Integer.sum(userCredit, credit)));
            userResource.update(userRepresentation);
        } catch (Exception e) {
            System.out.println(e);
        }

        return;
    }

    public String getUserAttribute(String userID, String attribute) {
        userRepresentation = getUserResource(userID).toRepresentation();
        return userRepresentation.firstAttribute(attribute);
    }

    public UserResource getUserResource(String userID) {
        RealmResource rs = keycloak.realm("liquid");
        return userResource = rs.users().get(userID);
    }

}
