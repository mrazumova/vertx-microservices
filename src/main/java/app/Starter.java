package app;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Starter {

    public static void main(String[] args) {
        startInClusterMode("app.a.VerticleA");
        startInClusterMode("app.b.VerticleB");
        startInClusterMode("app.c.VerticleC");
        startInClusterMode("app.d.VerticleD");
    }

    private static void startInClusterMode(String verticleName) {
        ClusterManager clusterManager = new HazelcastClusterManager();
        VertxOptions vertxOptions = new VertxOptions().setClusterManager(clusterManager);

        Vertx.rxClusteredVertx(vertxOptions).subscribe(
                vertx -> {
                    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
                    configRetriever.rxGetConfig().subscribe(
                            jsonObject -> {
                                DeploymentOptions deploymentOptions = new DeploymentOptions();
                                deploymentOptions.setConfig(jsonObject);

                                vertx.rxDeployVerticle(verticleName, deploymentOptions).subscribe(
                                        id -> System.out.println(verticleName + " deployment id : " + id),
                                        err -> System.out.println("ERROR : " + verticleName + " - " + err.getMessage())
                                );
                            }
                    );
                }
        );
    }

}
