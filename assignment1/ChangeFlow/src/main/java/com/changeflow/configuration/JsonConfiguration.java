package com.changeflow.configuration;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.config.annotation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class JsonConfiguration implements WebMvcConfigurer {
    @Override public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // Reject incompatible encodings before the controller can commit a mutation.
        // Filter the Accept alternatives as well: a UTF-8 fallback must not select UTF-16 later.
        configurer.strategies(List.of(new HeaderContentNegotiationStrategy() {
            @Override public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
                var acceptable = super.resolveMediaTypes(request).stream()
                        .filter(type -> type.getCharset() == null || StandardCharsets.UTF_8.equals(type.getCharset())).toList();
                if (acceptable.isEmpty()) throw new HttpMediaTypeNotAcceptableException(
                        List.of(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)));
                return acceptable;
            }
        }));
    }
    @Bean Jackson2ObjectMapperBuilderCustomizer changeFlowJson() {
        return builder -> builder.modulesToInstall(JavaTimeModule.class)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                        MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .deserializerByType(String.class, new JsonDeserializer<String>() {
                    @Override public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                        if (!parser.hasToken(JsonToken.VALUE_STRING)) return (String) context.handleUnexpectedToken(String.class, parser);
                        return parser.getText().strip();
                    }
                });
    }
}
