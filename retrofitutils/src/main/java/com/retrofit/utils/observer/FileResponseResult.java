package com.retrofit.utils.observer;

public interface FileResponseResult {

    void onSuccess();

    void onFailure(String code, String content);
}
