package com.blade.plugin.swagger.toolkit;

import com.blade.Environment;
import com.blade.kit.ReflectKit;
import com.blade.kit.StringKit;
import com.blade.mvc.RouteContext;
import com.blade.mvc.annotation.*;
import com.blade.mvc.http.HttpMethod;
import com.blade.mvc.http.HttpRequest;
import com.blade.mvc.http.HttpResponse;
import com.blade.mvc.multipart.FileItem;
import com.blade.mvc.route.Route;
import com.blade.plugin.swagger.annotation.ApiModel;
import com.blade.plugin.swagger.annotation.ApiModelProperty;
import com.blade.plugin.swagger.controller.SwaggerController;
import com.blade.plugin.swagger.model.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author darren
 * @date 2019/4/5
 */
@UtilityClass
@Slf4j
public class SwaggerKit {

    public List<SwaggerTag> tags(List<Route> routers) {
        return routers.stream()
                .map(Route::getTargetType)
                .filter(cls -> cls != SwaggerController.class)
                .distinct()
                .map(cls -> {
                    SwaggerTag tag = new SwaggerTag();
                    tag.setName(cls.getSimpleName());
                    if (cls.isAnnotationPresent(Path.class)) {
                        tag.setDescription(cls.getAnnotation(Path.class).description());
                    }
                    return tag;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Map<String, SwaggerPath>> paths(List<Route> routers) {
        return routers.stream()
                .filter(route -> route.getTargetType() != SwaggerController.class)
                .collect(Collectors.toMap(Route::getPath, route -> {
                    SwaggerPath swaggerPath = new SwaggerPath();
                    Method method = route.getAction();
                    if (method.isAnnotationPresent(com.blade.mvc.annotation.Route.class)) {
                        swaggerPath.setSummary(method.getAnnotation(com.blade.mvc.annotation.Route.class).description());
                    } else if (method.isAnnotationPresent(GetRoute.class)) {
                        swaggerPath.setSummary(method.getAnnotation(GetRoute.class).description());
                    } else if (method.isAnnotationPresent(PostRoute.class)) {
                        swaggerPath.setSummary(method.getAnnotation(PostRoute.class).description());
                    } else if (method.isAnnotationPresent(DeleteRoute.class)) {
                        swaggerPath.setSummary(method.getAnnotation(DeleteRoute.class).description());
                    } else if (method.isAnnotationPresent(PutRoute.class)) {
                        swaggerPath.setSummary(method.getAnnotation(PutRoute.class).description());
                    }
                    swaggerPath.setOperationId(route.getTargetType().getSimpleName() + "[" + route.getPath() + "]");
                    swaggerPath.setTags(Collections.singletonList(route.getTargetType().getSimpleName()));
                    swaggerPath.setResponses(responses(method.getGenericReturnType(), method.getReturnType()));

                    Type[] parameterTypes = method.getGenericParameterTypes();
                    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                    Parameter[] parameters = method.getParameters();
                    Class<?>[] parameterClass = method.getParameterTypes();
                    List<SwaggerParam> params = new ArrayList<>();
                    for (int i = 0; i < parameterTypes.length; i++) {
                        Type parameterType = parameterTypes[i];
                        Class parameterCls = parameterClass[i];
                        Annotation[] parameterAnnotation = parameterAnnotations[i];
                        if (parameterAnnotation.length > 0) {
                            List<? extends Class<? extends Annotation>> annotations = Arrays.stream(parameterAnnotation)
                                    .map(Annotation::annotationType)
                                    .collect(Collectors.toList());
                            SwaggerParam swaggerParam = new SwaggerParam();
                            if (annotations.contains(Param.class)) {
                                swaggerParam.setIn("query");
                                Param param = filterAnnotation(parameterAnnotation, Param.class);
                                if (StringKit.isNotBlank(param.name())) {
                                    swaggerParam.setName(param.name());
                                } else {
                                    swaggerParam.setName(parameters[i].getName());
                                }
                            } else if (annotations.contains(BodyParam.class)) {
                                swaggerParam.setIn("body");
                            } else if (annotations.contains(PathParam.class)) {
                                swaggerParam.setIn("path");
                                PathParam param = filterAnnotation(parameterAnnotation, PathParam.class);
                                if (StringKit.isNotBlank(param.name())) {
                                    swaggerParam.setName(param.name());
                                    swaggerParam.setRequired(Boolean.TRUE);
                                } else {
                                    swaggerParam.setName(parameters[i].getName());
                                }
                            } else if (annotations.contains(CookieParam.class)) {
                                swaggerParam.setIn("cookie");
                                CookieParam param = filterAnnotation(parameterAnnotation, CookieParam.class);
                                if (StringKit.isNotBlank(param.value())) {
                                    swaggerParam.setName(param.value());
                                } else {
                                    swaggerParam.setName(parameters[i].getName());
                                }
                            } else if (annotations.contains(HeaderParam.class)) {
                                swaggerParam.setIn("header");
                                HeaderParam param = filterAnnotation(parameterAnnotation, HeaderParam.class);
                                if (StringKit.isNotBlank(param.value())) {
                                    swaggerParam.setName(param.value());
                                } else {
                                    swaggerParam.setName(parameters[i].getName());
                                }
                            } else if (annotations.contains(MultipartParam.class)) {
                                swaggerParam.setIn("formData");
                                MultipartParam param = filterAnnotation(parameterAnnotation, MultipartParam.class);
                                if (StringKit.isNotBlank(param.value())) {
                                    swaggerParam.setName(param.value());
                                } else {
                                    swaggerParam.setName(parameters[i].getName());
                                }
                            }
                            if (ReflectKit.isBasicType(parameterType)) {
                                swaggerParam.setType((parameterCls).getSimpleName().toLowerCase());
                            } else if (parameterType instanceof Class && (parameterCls).isEnum()) {
                                swaggerParam.setType("string");
                            } else if (parameterType instanceof Collection) {
                                swaggerParam.setType("array");
                                Type actualType = ((ParameterizedType) parameterType).getActualTypeArguments()[0];
                                swaggerParam.setItems(schema(actualType, (Class) actualType));
                            } else if (ReflectKit.isArray(parameterType) || parameterCls.getComponentType() != null) {
                                swaggerParam.setType("array");
                                swaggerParam.setItems(schema(parameterCls.getComponentType(), parameterCls.getComponentType()));
                            } else if (parameterType == FileItem.class) {
                                swaggerParam.setType("file");
                            } else {
                                swaggerParam.setType("object");
                                swaggerParam.setSchema(schema(parameterType,parameterCls));
                            }
                            if (swaggerParam.getIn() != null) {
                                params.add(swaggerParam);
                            }
                        }
                    }
                    swaggerPath.setParameters(params);

                    return Collections.singletonMap(convertMethod(route.getHttpMethod()), swaggerPath);
                }));
    }

    private String convertMethod(HttpMethod method) {
        switch (method) {
            case ALL:
            case GET:
            case TRACE:
            case CONNECT:
            case BEFORE:
            case AFTER:
                return HttpMethod.GET.toString().toLowerCase();
            default:
                return method.toString().toLowerCase();
        }
    }

    private <T extends Annotation> T filterAnnotation(Annotation[] annotations, Class<T> cls) {
        return Arrays.stream(annotations)
                .filter(anno -> anno.annotationType() == cls)
                .findFirst()
                .map(cls::cast)
                .orElseThrow(RuntimeException::new);
    }

    public Map<String, SwaggerDefinition> definitions(List<Route> routers) {
        Map<String, SwaggerDefinition> definitions = new HashMap<>();


        routers.stream()
                .map(Route::getAction)
                .distinct()
                .forEach(method -> {
                    Class<?>[] parameterClasses = method.getParameterTypes();
                    Type[] parameterTypes = method.getGenericParameterTypes();
                    //params
                    for (int i = 0; i < parameterTypes.length; i++) {
                        fillDefinitions(parameterClasses[i], parameterTypes[i], definitions);
                    }
                    //return
                    fillDefinitions(method.getReturnType(), method.getGenericReturnType(), definitions);
                });
        return definitions;
    }

    private Boolean excludeCls(Class cls) {
        return cls == null
                || ReflectKit.isBasicType(cls)
                || isDateType(cls)
                || cls.isEnum()
                || HttpRequest.class == cls
                || HttpResponse.class == cls
                || RouteContext.class == cls
                || Environment.class == cls
                || Object.class == cls
                || void.class == cls;
    }

    private void fillDefinitions(Class cls, Type type, Map<String, SwaggerDefinition> definitions) {
        if (!excludeCls(cls)) {
            String clsName = cls.getName();
            String title = cls.getSimpleName();
            Class parameterizedType = null;
            if (type instanceof ParameterizedType && Collection.class.isAssignableFrom(cls)) {
                Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Class actualClass = (Class) actualType;
                fillDefinitions(actualClass, actualType, definitions);
                return;
            } else if (type instanceof ParameterizedType) {
                Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Class actualClass = (Class) actualType;
                fillDefinitions(actualClass, actualType, definitions);
                clsName = type.getTypeName();
                title = cls.getSimpleName() + "[" + actualClass.getSimpleName() + "]";
                parameterizedType = actualClass;
            } else if (cls.isArray()) {
                cls = cls.getComponentType();
                clsName = cls.getName();
                title = cls.getSimpleName();
            }
            if (!definitions.containsKey(clsName)) {
                definitions.putIfAbsent(clsName, makeObjectDefinition(title, cls, parameterizedType, definitions));
            }
        }
    }

    private SwaggerDefinition makeObjectDefinition(String title, Class<?> cls, Class parameterizedType, Map<String, SwaggerDefinition> definitions) {
        SwaggerDefinition definition = new SwaggerDefinition();
        definition.setType("object");
        definition.setTitle(title);
        if (cls.isAnnotationPresent(ApiModel.class)) {
            definition.setDescription(cls.getAnnotation(ApiModel.class).value());
        }
        List<Field> fields = ReflectKit.loopFields(cls);
        Map<String, SwaggerProperty> properties = new HashMap<>(fields.size());
        List<String> required = new ArrayList<>();
        fields.forEach(field -> {
            if (!properties.containsKey(field.getName())) {
                SwaggerProperty property = new SwaggerProperty();
                Type genericType = field.getGenericType();
                if (ReflectKit.isBasicType(field.getType())) {
                    property.setType(field.getType().getSimpleName().toLowerCase());
                } else if (isDateType(field.getType())) {
                    property.setType("date");
                } else if (Enum.class.isAssignableFrom(field.getType())) {
                    property.setType("string");
                } else if (genericType instanceof ParameterizedType && Collection.class.isAssignableFrom(field.getType())) {
                    property.setType("array");
                    Type actualType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                    Class actualCls = (Class) actualType;
                    if (ReflectKit.isBasicType(actualType)) {
                        property.setItems(Collections.singletonMap("type", actualCls.getSimpleName()));
                    } else {
                        String actualTypeName = actualType.getTypeName();
                        property.setItems(schema(actualType,actualCls));
                        if (!definitions.containsKey(actualTypeName)) {
                            definitions.putIfAbsent(actualTypeName, makeObjectDefinition(actualCls.getSimpleName(), actualCls, null, definitions));
                        }
                    }
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    property.setType("object");
                    if (genericType instanceof ParameterizedType) {
                        Type actualType = ((ParameterizedType) genericType).getActualTypeArguments()[1];
                        Class actualCls = (Class) actualType;
                        if (ReflectKit.isBasicType(actualType)) {
                            property.setAdditionalProperties(Collections.singletonMap("type", (actualCls).getSimpleName()));
                        } else {
                            String actualTypeName = actualType.getTypeName();
                            property.setAdditionalProperties(Collections.singletonMap("$ref", "#/definitions/" + actualTypeName));
                            if (!definitions.containsKey(actualTypeName)) {
                                definitions.putIfAbsent(actualTypeName, makeObjectDefinition(actualCls.getSimpleName(), actualCls, null, definitions));
                            }
                        }
                    }
                } else {
                    property.setType("object");
                    if (!Objects.equals(field.getType().getName(), field.getGenericType().getTypeName()) && parameterizedType != null) {
                        property.set$ref("#/definitions/" + parameterizedType.getName());
                        if (!definitions.containsKey(parameterizedType.getName())) {
                            definitions.putIfAbsent(parameterizedType.getName(), makeObjectDefinition(parameterizedType.getSimpleName(), parameterizedType, null, definitions));
                        }
                    }
                }
                String name = field.getName();
                if (field.isAnnotationPresent(ApiModelProperty.class)) {
                    ApiModelProperty modelProperty = field.getAnnotation(ApiModelProperty.class);
                    property.setDescription(modelProperty.value());
                    if (StringKit.isNotBlank(modelProperty.name())) {
                        name = modelProperty.name();
                    }
                    if (modelProperty.required()) {
                        required.add(name);
                    }
                }
                properties.putIfAbsent(field.getName(), property);
            }
        });
        definition.setProperties(properties);
        definition.setRequired(required);
        return definition;
    }


    private Map<String, Object> responses(Type type, Class cls) {
        Map<String, Object> r200 = new HashMap<>(2);
        r200.put("description", "OK");
        if (type != null && type != void.class) {
            r200.put("schema", schema(type, cls));
        }
        return Collections.singletonMap("200", r200);
    }

    private Map<String, Object> schema(Type type, Class cls) {
        if (ReflectKit.isBasicType(cls)) {
            return Collections.singletonMap("type", cls.getSimpleName().toLowerCase());
        } else if (isDateType(cls)) {
            return Collections.singletonMap("type", "date");
        } else if (Collection.class.isAssignableFrom(cls)) {
            Map<String, Object> schema = new HashMap<>(2);
            schema.put("type", "array");
            if (type instanceof ParameterizedType) {
                Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Class actualClass = (Class) actualType;
                schema.put("items", schema(actualType, actualClass));
            }
            return schema;
        } else if (type instanceof ParameterizedType) {
            return Collections.singletonMap("$ref", "#/definitions/" + type.getTypeName());
        }
        return Collections.singletonMap("$ref", "#/definitions/" + type.getTypeName());
    }

    private Boolean isDateType(Type type) {
        if (type instanceof Class) {
            Class cls = (Class) type;
            return Date.class.isAssignableFrom(cls) || Temporal.class.isAssignableFrom(cls);
        }
        return Boolean.FALSE;
    }

    public String toJSON(Object obj) {
        StringBuilder json = new StringBuilder();
        if (obj == null) {
            json.append("null");
        } else if (obj instanceof String || obj.getClass().isEnum() || isDateType(obj.getClass())) {
            json.append("\"").append(obj).append("\"");
        } else if (ReflectKit.isBasicType(obj)) {
            json.append(obj);
        } else if (obj instanceof Collection) {
            json.append("[")
                    .append(((Collection) obj).stream()
                            .map(SwaggerKit::toJSON)
                            .collect(Collectors.joining(",")))
                    .append("]");
        } else if (obj instanceof Map) {
            Map map = (Map) obj;
            List<String> fragment = new ArrayList<>(map.size());
            map.forEach((k, v) -> {
                if (v != null) {
                    fragment.add(toJSON(k) + ":" + toJSON(v));
                }
            });
            json.append("{").append(String.join(",", fragment)).append("}");
        } else if (obj.getClass().isArray()) {
            json.append("[")
                    .append(Arrays.stream((Object[]) obj)
                            .map(SwaggerKit::toJSON)
                            .collect(Collectors.joining(",")))
                    .append("]");
        } else {
            List<Field> fields = ReflectKit.loopFields(obj.getClass());
            json.append("{")
                    .append(fields.stream()
                            .peek(field -> field.setAccessible(Boolean.TRUE))
                            .map(field -> {
                                try {
                                    Object val = field.get(obj);
                                    if (val != null) {
                                        return toJSON(field.getName()) + ":" + toJSON(val);
                                    }
                                } catch (IllegalAccessException e) {
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(",")))
                    .append("}");
        }
        return json.toString();
    }
}
