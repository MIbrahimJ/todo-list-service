package com.tradebyte.todo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI todoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Todo List Service API")
                        .description("A resilient backend service for managing todo lists")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Todo Service Team")
                                .email("support@todo-service.com")));
    }
}