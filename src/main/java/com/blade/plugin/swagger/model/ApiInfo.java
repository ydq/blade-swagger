package com.blade.plugin.swagger.model;

import com.blade.mvc.Const;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * @author darren
 * @date 2019/4/5
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiInfo {
    String title ="Blade";
    String version = Const.VERSION;
    String description = "A simple api doc powered by swagger";
}
