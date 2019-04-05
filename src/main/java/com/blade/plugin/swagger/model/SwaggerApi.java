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
public class SwaggerApi {
    String                                swagger = "2.0";
    ApiInfo                               info = new ApiInfo();
    String                                host;
    String                                basePath;
    List<SwaggerTag>                      tags;
    Map<String, Map<String, SwaggerPath>> paths;
    Map<String, SwaggerDefinition>        definitions;
}
