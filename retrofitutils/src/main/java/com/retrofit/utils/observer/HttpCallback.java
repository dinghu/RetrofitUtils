package com.retrofit.utils.observer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class HttpCallback<T> {
    public abstract void onError(String code, String message);

    public abstract void onResponse(T response);

    protected Type genericityType;

    public HttpCallback() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            this.genericityType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        } else {
            this.genericityType = Object.class;
        }
    }

    public Type getGenericityType() {
        return genericityType;
    }

}
