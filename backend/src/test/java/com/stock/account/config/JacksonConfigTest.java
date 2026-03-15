package com.stock.account.config;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SpringBootTest
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("LocalDateTime을 ISO 8601 문자열로 직렬화한다")
    void serializeLocalDateTimeAsIsoString() throws Exception {
        record DateDto(LocalDateTime createdAt) {}
        LocalDateTime dateTime = LocalDateTime.of(2026, 3, 15, 14, 30, 0);

        String json = objectMapper.writeValueAsString(new DateDto(dateTime));

        assertThat(json).contains("2026-03-15T14:30:00");
        assertThat(json).doesNotContain("[2026");
    }

    @Test
    @DisplayName("null 필드는 JSON에서 제외한다")
    void excludeNullFields() throws Exception {
        record NullDto(String name, String description) {}

        String json = objectMapper.writeValueAsString(new NullDto("test", null));

        assertThat(json).contains("name");
        assertThat(json).doesNotContain("description");
    }

    @Test
    @DisplayName("알 수 없는 속성이 있어도 역직렬화에 실패하지 않는다")
    void deserializeWithUnknownProperties() {
        record SimpleDto(String name) {}
        String json = "{\"name\":\"test\",\"unknownField\":\"value\"}";

        assertThatNoException().isThrownBy(() ->
                objectMapper.readValue(json, SimpleDto.class)
        );
    }
}
