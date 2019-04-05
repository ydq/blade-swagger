package com.blade.plugin.swagger.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

/**
 * @author darren
 * @date 2019/4/5
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwaggerDefinition {
    String                       type;
    Map<String, SwaggerProperty> properties;
    String                       title;
    String                       description;
    List<String>                 required;
}
