package app.c;

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

public class VerticleC extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(VerticleC.class);

    public void start(Promise<Void> startPromise) throws Exception {
        Router router = Router.router(vertx);

        router.get("/user").handler(this::handle);

        router.get().handler(
                context -> context
                        .put("id", "value")
                        .reroute("/user")
        );

        int port = config().getInteger("c.port");
        String host = config().getString("c.host");
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router)
                .listen(port, host);

        logger.info("Listening on " + host + " " + port);
    }

    private void handle(RoutingContext context) {
        logger.info("Incoming request...");
        int id = Integer.parseInt(context.request().getParam("id"));

        JsonObject object = UserStorage.getCountryById(id);
        HttpServerResponse response = context.response();
        response.setStatusCode(200);
        response.setChunked(true);
        response.write(object.encode());
        response.end();
        logger.info("Parameter: " + id + " Response: " + object.toString());
    }
}
