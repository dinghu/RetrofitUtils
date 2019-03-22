package com.retrofit.utils.interceptor;

import java.util.List;

public interface RequestInterceptor {
    boolean preHandle(RequestInterceptorListener requestInterceptorListener);

    //需要排除的网络路径
    List<String> excludedUrls();

    interface RequestInterceptorListener {
        void onContinue();

        void onInterrupt(String message);
    }

}
