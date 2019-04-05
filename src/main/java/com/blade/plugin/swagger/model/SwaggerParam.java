package com.blade.plugin.swagger.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

import java.util.Map;

/**
 * @author darren
 * @date 2019/4/5
 */
@Data
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwaggerParam extends SwaggerProperty {
    String              in;
    String              name;
    boolean             required;
    String              type;
    Map<String, Object> schema;
}
