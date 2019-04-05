package com.blade.plugin.swagger.bootstrap;

import com.blade.Blade;
import com.blade.ioc.annotation.Bean;
import com.blade.loader.BladeLoader;
import com.blade.mvc.WebContext;
import com.blade.mvc.route.Route;
import com.blade.plugin.swagger.controller.SwaggerController;
import com.blade.plugin.swagger.model.SwaggerApi;
import com.blade.plugin.swagger.toolkit.SwaggerKit;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author darren
 * @date 2019/4/5
 */
@Bean
public class SwaggerBootstrap implements BladeLoader {


    private static String api;


    @Override
    public void load(Blade blade) {
        List<Route> routers = WebContext.blade().routeMatcher().getRoutes().values().stream()
                .filter(route -> route.getTargetType() != SwaggerController.class)
                .collect(Collectors.toList());
        SwaggerApi api = new SwaggerApi();
        api.setPaths(SwaggerKit.paths(routers));
        api.setTags(SwaggerKit.tags(routers));
        api.setDefinitions(SwaggerKit.definitions(routers));
        SwaggerBootstrap.api = SwaggerKit.toJSON(api);
    }


    public static String api(){
        return api;
    }
}
