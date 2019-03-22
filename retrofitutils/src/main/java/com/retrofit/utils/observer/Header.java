package com.retrofit.utils.observer;

import java.util.Map;

public interface Header {
    Map<String, Object> generateHeaderMap(String url);

    Map<String, Object> signHeadersParams(String host, String url, Map<String, Object> header, Map<String, Object> parameters);

    void onEtag(String url, String etag);
}
