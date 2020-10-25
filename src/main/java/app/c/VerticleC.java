package app.c;

import app.NoSuchUserException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleC extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleC.class);

    private ServiceDiscovery serviceDiscovery;

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
        httpServer.requestHandler(router).listen(port, host);

        logger.info("Listening on " + host + " " + port);

        serviceDiscovery = ServiceDiscovery.create(vertx);
        Record record = HttpEndpoint.createRecord("c", host, port, "/");
        serviceDiscovery.publish(record, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.info("Verticle C : registration succeeded, " + asyncResult.result().toJson());
            } else {
                logger.error("Verticle C : registration failed - " + asyncResult.cause().getMessage());
            }
        });
    }

    private void handle(RoutingContext context) {
        logger.info("Incoming request...");
        int id = Integer.parseInt(context.request().getParam("id"));
        HttpServerResponse response = context.response();
        try {
            JsonObject object = new JsonObject().put("id", id);
            vertx.eventBus().request("/getCountry", object, asyncResult -> {
                if (asyncResult.succeeded()) {
                    JsonObject reply = (JsonObject) asyncResult.result().body();
                    response.setChunked(true);
                    response.write(reply.encode());
                    logger.info("Parameter: " + id + " Response: " + reply.toString());
                }
            });
            response.end();
        } catch (NoSuchUserException e) {
            logger.info(e.getMessage());
            response.end(e.getMessage());
        }
    }
}
