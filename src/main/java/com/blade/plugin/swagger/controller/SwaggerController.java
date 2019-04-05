package com.blade.plugin.swagger.controller;

import com.blade.mvc.RouteContext;
import com.blade.mvc.annotation.GetRoute;
import com.blade.mvc.annotation.Path;
import com.blade.mvc.annotation.PathParam;
import com.blade.plugin.swagger.bootstrap.SwaggerBootstrap;

/**
 * @author darren
 * @date 2019/4/5
 */
@Path("/swagger")
public class SwaggerController {

    @GetRoute({"","/ui"})
    public String ui(){
        return "swagger/index.html";
    }

    @GetRoute("/oauth2-redirect")
    public String oauth2(){
        return "swagger/oauth2-redirect.html";
    }

    @GetRoute("/api")
    public void api(RouteContext ctx){
        ctx.json(SwaggerBootstrap.api());
    }

}
