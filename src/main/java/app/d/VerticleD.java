package app.d;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.eventbus.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticleD extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(VerticleD.class);

    @Override
    public Completable rxStart() {
        EventBus eventBus = vertx.eventBus();

        MessageConsumer<JsonObject> consumer = eventBus.consumer("/test");
        Observable<Message<JsonObject>> observable = consumer.toObservable();
        observable.subscribe(
                message -> {
                    JsonObject json = new JsonObject().put("service-D", "communication through EventBus");
                    logger.info("Received message. ");
                    message.rxReplyAndRequest(json).subscribe();
                    logger.info("Reply : " + json);
                },
                error -> logger.error(error.getMessage()));

        consumer.completionHandler(res -> {
            if (res.succeeded()) {
                logger.info("Verticle D is running");
            } else {
                logger.error(res.cause().getMessage());
            }
        });

        return consumer.rxCompletionHandler();
    }
}
