package liquid;

import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HTTPUpgrade implements HttpUpgradeCheck {

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext ctx) {
        if (rejectUpgrade(ctx)) {
            return CheckResult.rejectUpgrade(401);
        }
        return CheckResult.permitUpgrade();
    }

    private boolean rejectUpgrade(HttpUpgradeContext ctx) {
        final String token = ctx.httpRequest().getHeader("Sec-WebSocket-Protocol");
        if (token == null || token.isBlank() || !token.contains("Authorization")) {
            return true;
        }
        return false;
    }
}