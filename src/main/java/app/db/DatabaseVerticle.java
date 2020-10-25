package app.db;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);

    private static final String SQL_CREATE_USER_TABLE = "CREATE TABLE IF NOT EXISTS USER (ID INTEGER IDENTITY primary key, USERNAME varchar(20) unique, COUNTRY varchar(2), AGE INTEGER)\";";

    private static final String SQL_GET_BY_ID = "SELECT * FROM USER WHERE ID = ?";

    private JDBCClient dbClient;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        dbClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:hsqldb:file:db/user")
                .put("driver_class", "org.hsqldb.jdbcDriver")
                .put("max_pool_size", 30));

        EventBus eventBus = vertx.eventBus();

        MessageConsumer<JsonObject> usernameConsumer = eventBus.consumer("/getUsername");
        MessageConsumer<JsonObject> countryConsumer = eventBus.consumer("/getCountry");
        MessageConsumer<JsonObject> ageConsumer = eventBus.consumer("/getAge");

        countryConsumer.handler(handle("country"));
        ageConsumer.handler(handle("age"));
        usernameConsumer.handler(handle("username"));

    }

    private Handler<Message<JsonObject>> handle(String field) {
        return handler -> {
            JsonObject object = handler.body();
            logger.info("Received message : " + object.toString());

            int id = Integer.parseInt(object.getString("id"));

            dbClient.getConnection(asyncResult -> {
                SQLConnection connection = asyncResult.result();
                connection.queryWithParams(
                        SQL_GET_BY_ID,
                        new JsonArray().add(id),
                        fetch -> {
                            if (fetch.succeeded()){
                                JsonObject res = fetch.result().getRows().get(0);
                                object.put(field, res.getValue(field));
                            }
                        });
            });
            handler.reply(object);
        };
    }

   /* private Future<Void> prepareDatabase() {
        Promise<Void> promise = Promise.promise();


        dbClient.getConnection(ar -> {
            if (ar.failed()) {
                logger.error("Could not open a database connection", ar.cause());
                promise.fail(ar.cause());
            } else {
                SQLConnection connection = ar.result();
                connection.execute(SQL_CREATE_USER_TABLE, create -> {});
                connection.execute("INSERT INTO USER VALUES (1, 'firstUser', 'DE', 18)", create -> {});
                connection.execute("INSERT INTO USER VALUES (2, 'secondUser', 'UK', 39)", create -> {});
                connection.execute("INSERT INTO USER VALUES (3, 'thirdUser', 'UK', 43)", create -> {});
                connection.execute("INSERT INTO USER VALUES (4, 'fourthUser', 'AT', 13)", create -> {});
                connection.execute("INSERT INTO USER VALUES (5, 'fifthUser', 'AT', 16)", create -> {});
                connection.execute("INSERT INTO USER VALUES (6, 'fiveUser', 'RU', 18)", create -> {});
                connection.execute("INSERT INTO USER VALUES (7, 'seventhUser', 'RU', 28)", create -> {});
                connection.execute("INSERT INTO USER VALUES (8, 'eighthUser', 'DE', 19)", create -> {});
                connection.execute("INSERT INTO USER VALUES (9, 'ninthUser', 'FR', 31)", create -> {});
                connection.execute("INSERT INTO USER VALUES (10, 'tenthUser', 'AT', 74)", create -> {});
                connection.close();
            }
        });

        return promise.future();
    }*/
}
