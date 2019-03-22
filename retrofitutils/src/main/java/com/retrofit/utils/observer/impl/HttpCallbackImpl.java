package com.retrofit.utils.observer.impl;

import android.text.TextUtils;

import com.google.gson.JsonSyntaxException;
import com.retrofit.utils.observer.HttpCallback;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HttpCallbackImpl<T> implements Callback<T> {
    private HttpCallback httpCallback;

    public HttpCallbackImpl(HttpCallback httpCallback) {
        this.httpCallback = httpCallback;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful()) {
            httpCallback.onResponse(response.body());
        } else {
            try {
                ResponseBody errorBody = response.errorBody();
                if (errorBody != null) {
                    httpCallback.onError(String.valueOf(response.code()), errorBody.string());
                }
            } catch (Exception e) {
                e.printStackTrace();
                httpCallback.onError(String.valueOf(-1), e.getMessage());
            }
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable e) {
        int code = -1;
        if (e instanceof ConnectException || e instanceof SocketTimeoutException
                || e instanceof UnknownHostException) {
            code = 1009;
        } else if (e instanceof JsonSyntaxException) {
            code = 501;
        }
        String error = e.getMessage();
        if (call.isCanceled()) {
            error = "取消请求";
        }
        if (TextUtils.isEmpty(e.getMessage())) {
            error = "服务器未知异常或网络异常";
        }
        httpCallback.onError(String.valueOf(code), error);

    }
}
