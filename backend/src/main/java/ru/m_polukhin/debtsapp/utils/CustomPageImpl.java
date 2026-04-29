package ru.m_polukhin.debtsapp.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public class CustomPageImpl<T> extends PageImpl<T> {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CustomPageImpl(@JsonProperty("content") List<T> content, @JsonProperty("number") int number,
                          @JsonProperty("pageable") JsonNode pageable, @JsonProperty("last") boolean last,
                          @JsonProperty("totalElements") long totalElements, @JsonProperty("sort") JsonNode sort,
                          @JsonProperty("size") int size, @JsonProperty("numberOfElements") int numberOfElements) {
        super(content, PageRequest.of(number, size > 0 ? size : Math.max(numberOfElements, 1)), totalElements);
    }

    public CustomPageImpl(List<T> content) {
        super(content);
    }

}
