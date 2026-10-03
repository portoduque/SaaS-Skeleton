package dev.portoduque.saas.shared.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI publicApi() {
        return new OpenAPI().info(new Info()
                .title("SaaS-Skeleton API")
                .description("Frontend-agnostic HTTP API for SaaS-Skeleton")
                .version("v1"));
    }
}
