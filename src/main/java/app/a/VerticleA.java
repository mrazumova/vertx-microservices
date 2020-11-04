package app.a;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleA extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleA.class);

    private ServiceDiscovery serviceDiscovery;

    @Override
    public Completable rxStart() {
        Router router = Router.router(vertx);
        router.get("/user").handler(this::handle);
        router.get().handler(context -> context.put("id", "value").reroute("/user"));

        int port = config().getInteger("a.port");
        String host = config().getString("a.host");

        runServiceDiscovery(port, host);

        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router);

        logger.info("Listening on " + host + " " + port);

        return httpServer.rxListen(port, host).ignoreElement();
    }

    private void runServiceDiscovery(int port, String host) {
        serviceDiscovery = ServiceDiscovery.create(vertx);
        Record record = HttpEndpoint.createRecord("a", host, port, "/");
        serviceDiscovery.rxPublish(record).subscribe(
                json -> logger.info("Verticle A : registration succeeded, " + json),
                error -> logger.error("Verticle A : registration failed - " + error.getMessage()));
    }


    private void handle(RoutingContext context) {
        String id = context.request().getParam("id");

        Single<Record> serviceB = getServiceReference(new JsonObject().put("name", "b")).subscribeOn(Schedulers.computation());
        Single<Record> serviceC = getServiceReference(new JsonObject().put("name", "c")).subscribeOn(Schedulers.computation());

        Single<JsonObject> resultFromB = sendRequestToService(id, serviceB.blockingGet()).subscribeOn(Schedulers.computation());
        Single<JsonObject> resultFromC = sendRequestToService(id, serviceC.blockingGet()).subscribeOn(Schedulers.computation());

        Single
                .zip(resultFromB, resultFromC, JsonObject::mergeIn)
                .subscribe(
                        json -> {
                            if (json.isEmpty()) {
                                HttpServerResponse response = context.response();
                                response.setStatusCode(200);
                                JsonObject object = new JsonObject().put("error", "no user with id " + id);
                                response.end(object.encode());
                            } else {
                                Single<JsonObject> message = getMessageThroughEventBus(id, json).subscribeOn(Schedulers.computation());
                                message.subscribe(
                                        jsonMessage -> {
                                            HttpServerResponse response = context.response();
                                            response.setStatusCode(200);
                                            response.end(jsonMessage.encode());
                                            logger.info("OK");
                                        },
                                        error -> errorResponse(context, error)
                                );
                            }
                        },
                        error -> errorResponse(context, error)
                );
    }

    private Single<Record> getServiceReference(JsonObject filter) {
        return serviceDiscovery.rxGetRecord(filter).toSingle();
    }

    private Single<JsonObject> sendRequestToService(String id, Record record) {
        ServiceReference serviceReference = serviceDiscovery.getReference(record);
        WebClient webClient = serviceReference.getAs(WebClient.class);
        return webClient.get("/user")
                .addQueryParam("id", id)
                .as(BodyCodec.jsonObject())
                .rxSend()
                .map(HttpResponse::body);
    }

    private Single<JsonObject> getMessageThroughEventBus(String id, JsonObject jsonD) {
        jsonD.put("id", id);
        EventBus eventBus = vertx.eventBus();
        return eventBus.rxRequest("/user", jsonD).map(objectMessage -> (JsonObject) objectMessage.body());
    }

    private void errorResponse(RoutingContext context, Throwable error){
        logger.error(error.getMessage());
        HttpServerResponse response = context.response();
        response.setStatusCode(500);
        response.end();
    }

}
