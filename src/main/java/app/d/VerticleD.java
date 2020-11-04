package app.d;

import app.UserStorage;
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

        MessageConsumer<JsonObject> consumer = eventBus.consumer("/user");
        Observable<Message<JsonObject>> observable = consumer.toObservable();
        observable.subscribe(
                message -> {
                    JsonObject json = message.body();
                    logger.info("Received message : " + json);
                    int id = Integer.parseInt(json.getString("id"));
                    json.mergeIn(UserStorage.getAge(id));
                    message.rxReplyAndRequest(json).subscribe();
                    logger.info("Reply : " + json);
                });

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
