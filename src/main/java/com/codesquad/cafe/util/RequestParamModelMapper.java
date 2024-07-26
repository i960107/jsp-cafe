package com.codesquad.cafe.util;

import com.codesquad.cafe.exception.ModelMappingException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestParamModelMapper {

    private static Logger log = LoggerFactory.getLogger(RequestParamModelMapper.class);

    private RequestParamModelMapper() {
    }

    public static <T> T map(Map<String, String[]> requestParameters, Class<T> type) {
        try {
            T model = type.getDeclaredConstructor().newInstance();

            for (Field field : type.getDeclaredFields()) {
                if (!requestParameters.containsKey(field.getName())) {
                    continue;
                }
                String[] values = requestParameters.get(field.getName());
                field.setAccessible(true);

                if (List.class.isAssignableFrom(field.getType())) {
                    Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    List<Object> convertedValues = new ArrayList<>();
                    for (String value : values) {
                        Object convertedValue = castValue((Class<?>) genericType, value);
                        convertedValues.add(convertedValue);
                    }
                    field.set(model, convertedValues);
                } else if (values.length == 1) {
                    String value = values[0];
                    Object castedValue = castValue(field.getType(), value);
                    field.set(model, castedValue);
                } else {
                    throw new IllegalArgumentException("Field type not supported: " + field.getType());
                }
            }
            return model;
        } catch (Exception e) {
            log.warn("fail to map parameters to model {} : {} ", type.getName(), e);
            throw new ModelMappingException("fail to map parameters to model " + type.getName());
        }
    }

    private static Object castValue(Class<?> targetType, String value) throws IllegalArgumentException {
        if (targetType.equals(String.class)) {
            return value;
        } else if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
            return Integer.parseInt(value);
        } else if (targetType.equals(double.class) || targetType.equals(Double.class)) {
            return Double.valueOf(value);
        } else if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
            return Boolean.valueOf(value);
        } else if (targetType.equals(long.class) || targetType.equals(Long.class)) {
            return Long.valueOf(value);
        } else {
            throw new IllegalArgumentException("Unsupported target type: " + targetType);
        }
    }
}
