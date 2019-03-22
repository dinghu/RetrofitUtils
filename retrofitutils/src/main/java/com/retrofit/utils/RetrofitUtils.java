package com.retrofit.utils;

import com.retrofit.utils.observer.FileResponseResult;
import com.retrofit.utils.observer.HttpCallback;

import java.util.List;
import java.util.Map;

public class RetrofitUtils {
    private Configuration configuration;
    private RetrofitHelper retrofitHelper;

    public Configuration getConfiguration() {
        return configuration;
    }

    public RetrofitUtils(Configuration configuration) {
        this.configuration = configuration;
        retrofitHelper = new RetrofitHelper(configuration);
    }

    public <T> void post(String url, HttpCallback<T> callback) {
        retrofitHelper.post(url, null, callback);
    }

    public <T> void post(String url, Map<String, Object> parameters, HttpCallback<T> callback) {
        retrofitHelper.post(url, parameters, callback);
    }

    public <T> void postJson(String url, Object t, HttpCallback<T> callback) {
        retrofitHelper.postJson(url, t, callback);
    }

    public <T> void put(String url, HttpCallback<T> callback) {
        put(url, null, callback);
    }

    public <T> void put(String url, Map<String, Object> parameters, HttpCallback<T> callback) {
        retrofitHelper.put(url, parameters, callback);
    }

    public <T> void putJson(String url, Object body, HttpCallback<T> callback) {
        retrofitHelper.putJson(url, body, callback);
    }


    public <T> void delete(String url, HttpCallback<T> callback) {
        delete(url, null, callback);
    }

    public <T> void delete(String url, Map<String, Object> parameters, HttpCallback<T> callback) {
        retrofitHelper.delete(url, parameters, callback);
    }

    public <T> void deleteJson(String url, Object object, HttpCallback<T> callback) {
        retrofitHelper.deleteJson(url, object, callback);
    }

    public <T> void postFullPath(String fullUrl, HttpCallback<T> callback) {
        postFullPath(fullUrl, null, callback);
    }

    public <T> void postFullPath(String fullUrl, Map<String, Object> parameters, HttpCallback<T> callback) {
        retrofitHelper.postFullPath(fullUrl, parameters, callback);
    }

    public <T> void get(String url, final HttpCallback<T> callback) {
        get(url, null, callback);
    }

    public <T> void get(String url, Map<String, Object> parameters, HttpCallback<T> callback) {
        retrofitHelper.get(url, parameters, callback);
    }

    public <T> void getFullPath(String fullUrl, HttpCallback<T> callback) {
        getFullPath(fullUrl, null, callback);
    }

    public <T> void getFullPath(String fullUrl, Map<String, Object> parameters, HttpCallback<T> callback) {
        retrofitHelper.getFullPath(fullUrl, parameters, callback);
    }

    public void uploadFile(String url, String filePath, String fileDes, boolean isFullUrl, final FileResponseResult callback) {
        retrofitHelper.uploadFile(url, filePath, fileDes, isFullUrl, callback);

    }


    public void uploadFiles(String url, List<String> filePathList, boolean isFullUrl, final FileResponseResult callback) {
        retrofitHelper.uploadFiles(url, filePathList, isFullUrl, callback);
    }

    public <T> void syncPost(String url, Map<String, Object> parameters, HttpCallback<T> callback) {
        retrofitHelper.syncPost(url, parameters, callback);
    }

    public <T> void syncGet(String url, HttpCallback<T> callback) {
        syncGet(url, null, callback);
    }

    public <T> void syncGet(String url, Map<String, Object> parameters, HttpCallback<T> callback) {
        retrofitHelper.syncGet(url, parameters, callback);
    }

}