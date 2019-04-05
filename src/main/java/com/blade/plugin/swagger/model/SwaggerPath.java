package com.blade.plugin.swagger.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author darren
 * @date 2019/4/5
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwaggerPath {
    List<String>        tags;
    String              summary;
    String              operationId;
    List<String>        consumes = Arrays.asList("application/json", "application/xml", "*/*");
    List<String>        produces = Arrays.asList("application/json", "application/xml", "*/*");
    List<SwaggerParam>  parameters;
    Map<String, Object> responses;
}
