package com.blade.plugin.swagger.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Map;

/**
 * @author darren
 * @date 2019/4/5
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwaggerProperty{
    String              type;
    String              description;
    Map<String, Object> items;
    Map<String, String> additionalProperties;
    String              $ref;

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }
}
