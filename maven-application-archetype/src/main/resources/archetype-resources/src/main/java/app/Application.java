package ${package}.app;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author ${author}
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
            .bannerMode(Banner.Mode.OFF)
            .web(WebApplicationType.SERVLET)
            .run(args);
    }
}
