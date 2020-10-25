package app.a;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleA extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleA.class);

    private ServiceDiscovery serviceDiscovery;

    public void start(Promise<Void> startPromise) throws Exception {
        Router router = Router.router(vertx);

        router.get("/user").handler(this::handle);

        router.get().handler(
                context -> context
                        .put("id", "value")
                        .reroute("/user")
        );

        int port = config().getInteger("a.port");
        String host = config().getString("a.host");
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router).listen(port, host);

        logger.info("Listening on " + host + " " + port);

        serviceDiscovery = ServiceDiscovery.create(vertx);
        Record record = HttpEndpoint.createRecord("a", host, port, "/");
        serviceDiscovery.publish(record, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.info("Verticle A : registration succeeded, " + asyncResult.result().toJson());
            } else {
                logger.error("Verticle A : registration failed - " + asyncResult.cause().getMessage());
            }
        });
    }

    private void handle(RoutingContext context) {
        String id = context.request().getParam("id");

        Promise<JsonObject> promiseResultB = Promise.promise();
        Promise<JsonObject> promiseResultC = Promise.promise();

        vertx.executeBlocking(
                findServiceAndSendRequest(id, promiseResultB, new JsonObject().put("name", "b")),
                false,
                asyncResult -> {
                }
        );

        vertx.executeBlocking(
                findServiceAndSendRequest(id, promiseResultC, new JsonObject().put("name", "c")),
                false,
                asyncResult -> {
                }
        );

        CompositeFuture compositeFuture = CompositeFuture.all(promiseResultB.future(), promiseResultC.future());
        compositeFuture.onComplete(result -> {
            if (result.succeeded()) {
                JsonObject jsonB = promiseResultB.future().result();
                JsonObject jsonC = promiseResultC.future().result();

                JsonObject jsonD = new JsonObject();
                jsonD.put("id", id);
                //jsonD.mergeIn(jsonB);
                //jsonD.mergeIn(jsonC);

                EventBus eventBus = vertx.eventBus();

                eventBus.request("/user", jsonD,
                        asyncResult -> {
                            if (asyncResult.succeeded()) {
                                JsonObject reply = (JsonObject) asyncResult.result().body();
                                HttpServerResponse response = context.response();
                                response.setStatusCode(200);
                                response.end(reply.encode());
                                logger.info("OK");
                            } else {
                                HttpServerResponse response = context.response();
                                response.setStatusCode(500);
                                response.end(asyncResult.cause().getMessage());
                                logger.error(asyncResult.cause().getMessage());
                            }
                        });

            } else {
                logger.error(result.cause().getMessage());
                HttpServerResponse response = context.response();
                response.setStatusCode(500);

                response.end();
            }
        });
    }

    private Handler<Promise<Object>> findServiceAndSendRequest(String id, Promise<JsonObject> promiseResult, JsonObject filter) {
        return future -> serviceDiscovery.getRecord(
                filter,
                asyncResult -> {
                    if (asyncResult.succeeded() && asyncResult.result() != null) {
                        ServiceReference serviceReference = serviceDiscovery.getReference(asyncResult.result());
                        WebClient webClient = serviceReference.getAs(WebClient.class);
                        sendRequest(id, promiseResult, future, webClient);
                        serviceReference.release();
                    }
                }
        );
    }

    private void sendRequest(String id, Promise<JsonObject> promiseResult, Promise<Object> future, WebClient webClient) {
        HttpRequest<Buffer> request = webClient.get("/user");
        request.addQueryParam("id", id);
        request.send(
                asyncResult -> {
                    if (asyncResult.succeeded()) {
                        try {
                            JsonObject jsonResult = asyncResult.result().body().toJsonObject();
                            promiseResult.complete(jsonResult);
                            future.complete();
                        } catch (Exception e) {
                            promiseResult.fail(e);
                            future.complete();
                        }
                    } else {
                        promiseResult.fail(asyncResult.cause());
                        future.fail(asyncResult.cause());
                    }
                });
    }
}
