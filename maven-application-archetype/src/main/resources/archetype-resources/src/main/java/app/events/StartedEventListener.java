package ${package}.app.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;

/**
 * @author ${author}
 */
@Configuration
public class StartedEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(StartedEventListener.class);

    @Async
    @Order
    @EventListener(WebServerInitializedEvent.class)
    public void afterStart(WebServerInitializedEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();
        int port = event.getWebServer().getPort();

        String profiles = StringUtils.arrayToCommaDelimitedString(environment.getActiveProfiles());
        String contextPath = event.getApplicationContext().getApplicationName();

        StringBuilder sb = new StringBuilder();
        sb.append("🚀 Server ready at http://localhost:").append(port);

        if (!StringUtils.isEmpty(contextPath)) {
            sb.append(contextPath);
        }

        LOG.info("✅ Active profiles: {}", profiles);
        LOG.info(sb.toString());
    }
}
