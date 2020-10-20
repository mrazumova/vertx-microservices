package app.a;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleA extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(VerticleA.class);

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
        httpServer.requestHandler(router)
                .listen(port, host);

        logger.info("Listening on " + host + " " + port);
    }

    private void handle(RoutingContext context) {
        String id = context.request().getParam("id");

        Promise<JsonObject> promiseResultB = Promise.promise();
        Promise<JsonObject> promiseResultC = Promise.promise();

        vertx.executeBlocking(
                future -> sendRequest(id, promiseResultB, future, config().getString("b.host"), config().getInteger("b.port")),
                false,
                asyncResult -> {
                }
        );

        vertx.executeBlocking(
                future -> sendRequest(id, promiseResultC, future, config().getString("c.host"), config().getInteger("c.port")),
                false,
                asyncResult -> {
                }
        );

        CompositeFuture
                .all(promiseResultB.future(), promiseResultC.future())
                .onComplete(result -> {
                    if (result.succeeded()) {
                        JsonObject jsonB = promiseResultB.future().result();
                        JsonObject jsonC = promiseResultC.future().result();

                        JsonObject jsonD = new JsonObject();
                        jsonD.put("id", id);
                        jsonD.mergeIn(jsonB);
                        jsonD.mergeIn(jsonC);

                        EventBus eventBus = vertx.eventBus();

                        eventBus.request("/user", jsonD,
                                asyncResult -> {
                                    if (asyncResult.succeeded()) {
                                        JsonObject reply = (JsonObject) asyncResult.result().body();
                                        HttpServerResponse response = context.response();
                                        response.setStatusCode(200);
                                        response.end(reply.encode());
                                    } else {
                                        HttpServerResponse response = context.response();
                                        response.setStatusCode(520);
                                        response.end(asyncResult.cause().getMessage());
                                    }
                                });

                    } else {
                        logger.error(result.cause().getMessage());
                    }
                });
    }

    private void sendRequest(String id, Promise<JsonObject> promiseResult, Promise<Object> future, String appHost, int appPort) {
        WebClient webClient = WebClient.create(vertx);
        HttpRequest<Buffer> request = webClient.get(appPort, appHost, "/user");
        request.addQueryParam("id", id);
        request.send(
                asyncResult -> {
                    if (asyncResult.succeeded()) {
                        JsonObject jsonResult = asyncResult.result().body().toJsonObject();
                        promiseResult.complete(jsonResult);
                        future.complete();
                    } else {
                        promiseResult.fail(asyncResult.cause());
                        future.fail(asyncResult.cause());
                    }
                });
    }
}
