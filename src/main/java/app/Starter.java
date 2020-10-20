package app;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Starter {

    public static void main(String[] args) {
        startInClusterMode("app.a.VerticleA");
        start("app.b.VerticleB");
        start("app.c.VerticleC");
        startInClusterMode("app.d.VerticleD");
    }

    private static void startInClusterMode(String verticleName) {
        Vertx vertx = Vertx.vertx();

        ClusterManager clusterManager = new HazelcastClusterManager();
        Vertx.clusteredVertx(new VertxOptions().setClusterManager(clusterManager), handler -> {
            ConfigRetriever retriever = ConfigRetriever.create(vertx);
            retriever.getConfig(
                    json -> {
                        JsonObject config = json.result();
                        DeploymentOptions deploymentOptions = new DeploymentOptions();

                        deploymentOptions.setConfig(config);
                        vertx.deployVerticle(verticleName, deploymentOptions);
                    }
            );
        });
    }

    private static void start(String verticleName) {
        Vertx vertx = Vertx.vertx();

        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(
                json -> {
                    JsonObject config = json.result();
                    DeploymentOptions deploymentOptions = new DeploymentOptions();

                    deploymentOptions.setConfig(config);
                    vertx.deployVerticle(verticleName, deploymentOptions);
                }
        );
    }

}
