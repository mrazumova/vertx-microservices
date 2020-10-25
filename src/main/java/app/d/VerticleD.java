package app.d;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleD extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleD.class);

    public void start(Promise<Void> startPromise) throws Exception {
        EventBus eventBus = vertx.eventBus();

        MessageConsumer<JsonObject> consumer = eventBus.consumer("/user");
        consumer.handler(handler -> {
            JsonObject object = handler.body();
            logger.info("Received message : " + object.toString());
            vertx.eventBus().request("/getAge", object, asyncResult -> {
                if (asyncResult.succeeded()) {
                    JsonObject reply = (JsonObject) asyncResult.result().body();
                    handler.reply(reply);
                    logger.info("Reply : " + reply.toString());
                } else {
                    logger.error(asyncResult.cause().getMessage());
                }
            });
        });

        consumer.completionHandler(res -> {
            if (res.succeeded()) {
                logger.info("Verticle D is running");
            } else {
                logger.error(res.cause().getMessage());
            }
        });
    }
}
