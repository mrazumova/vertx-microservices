package app.b;

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
        router.get("/test").handler(this::handle);

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
        HttpServerResponse response = context.response();
        JsonObject object = new JsonObject().put("service-B", "communication through HTTP");
        response.end(object.encode());
        logger.info("Response: " + object.toString());
    }
}
