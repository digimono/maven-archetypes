package ${package}.app.config;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ${author}
 */
@Configuration
public class ShutdownConfig {

    @Bean
    public GracefulShutdown gracefulShutdown() {
        return new GracefulShutdown();
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer(
        final GracefulShutdown gracefulShutdown) {
        return factory -> {
            if (factory instanceof TomcatServletWebServerFactory) {
                ((TomcatServletWebServerFactory) factory).addConnectorCustomizers(gracefulShutdown);
            }
        };
    }

    private static class GracefulShutdown implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

        private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdown.class);
        private static final int TIMEOUT = 60;

        private volatile Connector connector;

        @Override
        public void customize(Connector connector) {
            this.connector = connector;
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            if (this.connector == null) {
                return;
            }
            awaitTermination(this.connector);
        }

        private void awaitTermination(Connector connector) {
            connector.pause();

            Executor executor = connector.getProtocolHandler().getExecutor();
            if (executor instanceof ThreadPoolExecutor) {
                LOG.warn("Context closed. Going to await termination for {} seconds.", TIMEOUT);

                try {
                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                    threadPoolExecutor.shutdown();

                    if (!threadPoolExecutor.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                        LOG.warn(
                            "Tomcat thread pool did not shut down gracefully within {} seconds. Proceeding with forceful shutdown",
                            TIMEOUT);

                        threadPoolExecutor.shutdownNow();

                        if (!threadPoolExecutor.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                            LOG.error("Tomcat thread pool did not terminate");
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
