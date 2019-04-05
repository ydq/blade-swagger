package com.blade.plugin.swagger.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * @author darren
 * @date 2019/4/5
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwaggerTag {
    String name;
    String description;
}
