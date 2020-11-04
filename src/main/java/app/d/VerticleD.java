package app.d;

import app.UserStorage;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleD extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleD.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        EventBus eventBus = vertx.eventBus();

        MessageConsumer<JsonObject> consumer = eventBus.consumer("/user");
        consumer.handler(handler -> {
            JsonObject object = handler.body();
            logger.info("Received message : " + object.toString());
            int id = Integer.parseInt(object.getString("id"));
            object.mergeIn(UserStorage.getAge(id));
            handler.reply(object);
            logger.info("Reply : " + object.toString());
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
