package com.blade.plugin.swagger.model;

import com.blade.mvc.http.HttpMethod;

import java.lang.reflect.Method;

/**
 * @author darren
 * @date 2019/4/5
 */
public class RouteVO {
    String path;
    HttpMethod httpMethod;
    Class cls;
    Method method;
}
