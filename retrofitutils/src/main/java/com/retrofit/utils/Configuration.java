package com.retrofit.utils;

import android.content.Context;


import com.retrofit.utils.interceptor.RequestInterceptor;
import com.retrofit.utils.observer.Header;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
    private Context context;
    private String baseUrl;
    private Header headerMap;
    private List<RequestInterceptor> requestInterceptors;

    public Context getContext() {
        return context;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Header getHeaderMap() {
        return headerMap;
    }

    public List<RequestInterceptor> getRequestInterceptors() {
        return requestInterceptors;
    }

    private Configuration(Builder builder) {
        this.context = builder.context;
        this.baseUrl = builder.baseUrl;
        this.headerMap = builder.headerMap;
        this.requestInterceptors = builder.requestInterceptors;
    }


    public static final class Builder {
        private Context context;
        private String baseUrl;
        private Header headerMap;
        private List<RequestInterceptor> requestInterceptors = new ArrayList<>();

        public Builder() {
        }

        public Configuration build() {
            return new Configuration(this);
        }

        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder headerMap(Header headerMap) {
            this.headerMap = headerMap;
            return this;
        }

        public Builder addRequestInterceptor(RequestInterceptor interceptor) {
            this.requestInterceptors.add(interceptor);
            return this;
        }

    }
}
