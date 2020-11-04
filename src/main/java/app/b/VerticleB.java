package app.b;

import app.NoSuchUserException;
import app.UserStorage;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleB extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleB.class);

    private ServiceDiscovery serviceDiscovery;

    @Override
    public Completable rxStart() {
        Router router = Router.router(vertx);
        router.get("/user").handler(this::handle);
        router.get().handler(context -> context.put("id", "value").reroute("/user"));

        int port = config().getInteger("b.port");
        String host = config().getString("b.host");

        runServiceDiscovery(port, host);

        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router);

        logger.info("Listening on " + host + " " + port);

        return httpServer.rxListen(port, host).ignoreElement();
    }

    private void runServiceDiscovery(int port, String host) {
        serviceDiscovery = ServiceDiscovery.create(vertx);
        Record record = HttpEndpoint.createRecord("b", host, port, "/");
        serviceDiscovery.publish(record, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.info("Verticle B : registration succeeded, " + asyncResult.result().toJson());
            } else {
                logger.error("Verticle B : registration failed - " + asyncResult.cause().getMessage());
            }
        });
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
            response.end(new JsonObject().encode());
        }
    }
}
