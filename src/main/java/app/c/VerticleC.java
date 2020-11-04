package app.c;

import app.NoSuchUserException;
import app.UserStorage;
import io.vertx.core.Future;
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

public class VerticleC extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleC.class);

    private ServiceDiscovery serviceDiscovery;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Router router = Router.router(vertx);
        router.get("/user").handler(this::handle);
        router.get().handler(context -> context.put("id", "value").reroute("/user"));

        int port = config().getInteger("c.port");
        String host = config().getString("c.host");

        runServiceDiscovery(port, host);
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router);
        httpServer.rxListen(port, host).subscribe();

        logger.info("Listening on " + host + " " + port);
    }

    private void runServiceDiscovery(int port, String host) {
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
            JsonObject object = UserStorage.getCountryById(id);
            response.end(object.encode());
            logger.info("Parameter: " + id + " Response: " + object.toString());
        } catch (NoSuchUserException e) {
            logger.info(e.getMessage());
            response.end(e.getMessage());
        }
    }
}
