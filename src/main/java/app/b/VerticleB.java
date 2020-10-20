package app.b;

import app.NoSuchUserException;
import app.UserStorage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleB extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(VerticleB.class);

    public void start(Promise<Void> startPromise) throws Exception {
        Router router = Router.router(vertx);

        router.get("/user").handler(this::handle);

        router.get().handler(
                context -> context
                        .put("id", "value")
                        .reroute("/user")
        );

        int port = config().getInteger("b.port");
        String host = config().getString("b.host");
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router)
                .listen(port, host);

        logger.info("Listening on " + host + " " + port);
    }

    private void handle(RoutingContext context) {
        logger.info("Incoming request...");
        int id = Integer.parseInt(context.request().getParam("id"));
        HttpServerResponse response = context.response();
        try {
            JsonObject object = UserStorage.getUsernameById(id);
            response.end(object.encode());
            logger.info("Parameter: " + id + " Response: " + object.toString());
        } catch (NoSuchUserException e) {
            logger.info(e.getMessage());
            response.end(e.getMessage());
        }
    }
}
