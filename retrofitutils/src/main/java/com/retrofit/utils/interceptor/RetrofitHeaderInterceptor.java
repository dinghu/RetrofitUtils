package com.retrofit.utils.interceptor;

import android.text.TextUtils;

import com.retrofit.utils.Configuration;
import com.retrofit.utils.RetrofitUtils;
import com.retrofit.utils.observer.Header;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetrofitHeaderInterceptor implements Interceptor {

    private Configuration configuration;

    public RetrofitHeaderInterceptor(Configuration configuration) {
        this.configuration = configuration;
    }

    private void rebuildQueryParameters(HttpUrl url, HttpUrl.Builder urlBuilder, Map<String, Object> query) {
        Set<String> names = url.queryParameterNames();
        for (String name : names) {
            List<String> values = url.queryParameterValues(name);
            //TODO 移除该查询参数
            urlBuilder.removeAllQueryParameters(name);
            //todo 重新添加参数
            if (values != null && !values.isEmpty()) {
                for (String value : values) {
                    if (!TextUtils.isEmpty(value)) {
                        String tValue = value;
                        try {
                            tValue = URLDecoder.decode(tValue, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                        }
                        query.put(name, tValue);
                        urlBuilder.addQueryParameter(name, value);
                    }
                }
            }
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        if (configuration.getHeaderMap() == null) {
            return chain.proceed(original);
        }
        HttpUrl originalUrl = original.url();
        HttpUrl.Builder urlBuilder = originalUrl.newBuilder();

        Map<String, Object> query = new HashMap<>();
        //todo 去除无效参数
        rebuildQueryParameters(originalUrl, urlBuilder, query);

        HttpUrl newUrl = urlBuilder.build();
        Request.Builder builder = original.newBuilder().method(original.method(), original.body()).url(newUrl);

        //新的url
        String url = newUrl.toString();

        String baseUrl = configuration.getBaseUrl();
        Header header = configuration.getHeaderMap();
        //生成项目指定header
        Map<String, Object> headers = header.generateHeaderMap(url);
        //签名完善header
        Map<String, Object> headerTarget = header.signHeadersParams(baseUrl, url, headers, query);
        Set<Map.Entry<String, Object>> set = headerTarget.entrySet();
        for (Map.Entry<String, Object> entry : set) {
            if (entry != null && entry.getKey() != null & entry.getValue() != null) {
                builder.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }

        Request request = builder.build();
        Response response = chain.proceed(request);

        if (response.isSuccessful()) {
            header.onEtag(url, getEtagFromResponse(response.headers()));
        }
        return response;
    }

    private static String getEtagFromResponse(Headers reqheader) {
        if (null != reqheader && reqheader.size() > 0) {
            Object fileName = reqheader.get("Etag");
            if (null != fileName) {
                return fileName.toString();
            }
        }
        return null;
    }
}
