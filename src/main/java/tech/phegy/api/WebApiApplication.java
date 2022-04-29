package tech.phegy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tech.phegy.api.service.imageGenerator.ImageGeneratorProps;
import tech.phegy.api.service.jwt.JwtProps;
import tech.phegy.api.service.points.VoteProps;
import tech.phegy.api.service.register.RegisterProps;
import tech.phegy.api.service.storage.AwsStorageProps;

@SpringBootApplication()
@EnableConfigurationProperties({
        JwtProps.class,
        AwsStorageProps.class,
        RegisterProps.class,
        ImageGeneratorProps.class,
        VoteProps.class
})
public class WebApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApiApplication.class, args);
    }
}
