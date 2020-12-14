package app.c;

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

public class VerticleC extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleC.class);

    private ServiceDiscovery serviceDiscovery;

    @Override
    public Completable rxStart() {
        Router router = Router.router(vertx);
        router.get("/test").handler(this::handle);

        int port = config().getInteger("c.port");
        String host = config().getString("c.host");

        runServiceDiscovery(port, host);
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router);

        logger.info("Listening on " + host + " " + port);

        return httpServer.rxListen(port, host).ignoreElement();
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
        HttpServerResponse response = context.response();
        JsonObject object = new JsonObject().put("service-C", "communication through HTTP");
        response.end(object.encode());
        logger.info("Response: " + object.toString());
    }
}
