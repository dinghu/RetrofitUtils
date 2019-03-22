package com.retrofit.utils;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.retrofit.utils.interceptor.HttpLoggingInterceptor;
import com.retrofit.utils.interceptor.RequestInterceptor;
import com.retrofit.utils.interceptor.RetrofitHeaderInterceptor;
import com.retrofit.utils.observer.FileResponseResult;
import com.retrofit.utils.observer.HttpCallback;
import com.retrofit.utils.observer.impl.HttpCallbackImpl;
import com.retrofit.utils.upload.UploadFileRequestBody;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class RetrofitHelper {
    private Retrofit mInstance;
    private final long READ_TIMEOUT_MILLIS = 30;
    private final long WRITE_TIMEOUT_MILLIS = 30;
    private final long CACHESIZE = 10 * 1024 * 1024;
    private CommonCall commonCall;
    private Gson gson = new Gson();
    private Configuration configuration;


    public Retrofit getRetrofit() {
        return mInstance;
    }

    public RetrofitHelper(Configuration configuration) {
        initRetrofit(configuration);
    }

    public Retrofit initRetrofit(Configuration configuration) {
        File httpCacheDirectory = new File(configuration.getContext().getExternalCacheDir(), "responses");
        if (httpCacheDirectory != null && !httpCacheDirectory.exists()) {
            httpCacheDirectory.mkdirs();
        }
        Cache cache = null;
        if (httpCacheDirectory.exists()) {
            cache = new Cache(httpCacheDirectory, CACHESIZE);
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(READ_TIMEOUT_MILLIS, TimeUnit.SECONDS);
        builder.readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.SECONDS);
        builder.writeTimeout(WRITE_TIMEOUT_MILLIS, TimeUnit.SECONDS);
        builder.cache(cache);
        builder.addInterceptor(new RetrofitHeaderInterceptor(configuration));
        builder.addInterceptor(getHttpLoggingInterceptor());
        OkHttpClient client = builder.build();

        mInstance = new Retrofit.Builder()
                .baseUrl(configuration.getBaseUrl())
                .client(client)
                .build();

        commonCall = create(CommonCall.class);

        return mInstance;
    }


    public HttpLoggingInterceptor getHttpLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                if (!TextUtils.isEmpty(message)) {
                    Log.i("http", message);
                }
            }
        });
        httpLoggingInterceptor.setLogRequetBody(true);
        httpLoggingInterceptor.setLogResponseBody(true);
        return httpLoggingInterceptor;
    }

    private <T> boolean doInterceptor(final Call<T> call, final HttpCallback<T> observer) {
        List<RequestInterceptor> requestInterceptors = configuration.getRequestInterceptors();
        String url = call.request().url().toString();
        for (RequestInterceptor interceptor : requestInterceptors) {
            List<String> excludedUrls = interceptor.excludedUrls();
            // 是否不需要拦截
            boolean isexcludedUrl = false;
            for (String s : excludedUrls) {
                if (url.contains(s)) {
                    isexcludedUrl = true;
                    break;
                }
            }
            if (isexcludedUrl) {
                continue;
            }
            if (interceptor.preHandle(new RequestInterceptor.RequestInterceptorListener() {
                @Override
                public void onContinue() {
                    //继续处理
                    call.enqueue(new HttpCallbackImpl(observer));
                }

                @Override
                public void onInterrupt(String message) {
                    if (observer != null) {
                        observer.onError("-1", message);
                    }
                }
            })) {
                return true;
            }
        }

        return false;
    }

    private <T> void syncCall(Call<ResponseBody> call, HttpCallback<T> callback) {
        try {
            Response<ResponseBody> response = call.execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                Type type = callback.getGenericityType();
                if (type == String.class) {
                    callback.onResponse((T) body);
                } else {
                    T target = gson.fromJson(body, type);
                    callback.onResponse(target);
                }
            } else {
                callback.onError("-1", response.message());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <T> void enqueueCall(Call<ResponseBody> call, final HttpCallback<T> observer) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String body = response.body().string();
                        Type type = observer.getGenericityType();
                        if (type == String.class) {
                            observer.onResponse((T) body);
                        } else {
                            T target = gson.fromJson(body, type);
                            observer.onResponse(target);
                        }

                    } else {
                        observer.onError("" + response.code(), response.errorBody().string());
                    }

                } catch (Exception e) {
                    observer.onError("" + response.code(), e.getMessage());
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                observer.onError("-1", t.getMessage());
            }
        });
    }


    public <T> void post(String url, Map<String, Object> parameters, HttpCallback<T> observer) {
        if (parameters == null || parameters.isEmpty()) {
            Call<ResponseBody> call = commonCall.doPost(url);
            enqueueCall(call, observer);
        } else {
            Call<ResponseBody> call = commonCall.doPost(url, parameters);
            enqueueCall(call, observer);
        }

    }

    public <T> void putJson(String url, Object body, HttpCallback<T> callback) {
        String parameters = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters);
        Call<ResponseBody> call = commonCall.doPut(url, requestBody);
        enqueueCall(call, callback);
    }

    public <T> void put(String url, Map<String, Object> parameters, HttpCallback<T> callback) {

        if (parameters == null || parameters.isEmpty()) {
            Call<ResponseBody> call = commonCall.doPut(url);
            enqueueCall(call, callback);
        } else {
            Call<ResponseBody> call = commonCall.doPut(url, parameters);
            enqueueCall(call, callback);
        }
    }

    public <T> void delete(String url, Map<String, Object> parameters, HttpCallback<T> callback) {

        if (parameters == null || parameters.isEmpty()) {
            Call<ResponseBody> call = commonCall.doDelete(url);
            enqueueCall(call, callback);
        } else {
            Call<ResponseBody> call = commonCall.doDelete(url, parameters);
            enqueueCall(call, callback);
        }
    }

    public <T> void deleteJson(String url, Object body, HttpCallback<T> callback) {
        String parameters = new Gson().toJson(body);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters);
        Call<ResponseBody> call = commonCall.doDelete(url, requestBody);
        enqueueCall(call, callback);
    }

    public <T> void postJson(String url, Object t, HttpCallback<T> callback) {
        String parameters = new Gson().toJson(t);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters);
        Call<ResponseBody> call = commonCall.doPost(url, body);
        enqueueCall(call, callback);
    }

    public <T> void syncPost(String url, Map<String, Object> parameters, HttpCallback<T> callback) {
        Call<ResponseBody> call = commonCall.doPost(url, parameters);
        syncCall(call, callback);
    }

    public <T> void postFullPath(String fullUrl, Map<String, Object> parameters, HttpCallback<T> callback) {
        Call<ResponseBody> call = commonCall.doPostFullPath(fullUrl, parameters);
        enqueueCall(call, callback);
    }

    public <T> void get(String url, Map<String, Object> parameters, HttpCallback<T> callback) {

        if (parameters == null || parameters.isEmpty()) {
            Call<ResponseBody> call = commonCall.doGet(url);
            enqueueCall(call, callback);
        } else {
            Call<ResponseBody> call = commonCall.doGet(url, parameters);
            enqueueCall(call, callback);
        }
    }


    public <T> void syncGet(String url, Map<String, Object> parameters, HttpCallback<T> callback) {
        if (parameters == null || parameters.isEmpty()) {
            Call<ResponseBody> call = commonCall.doGet(url);
            syncCall(call, callback);
        } else {
            Call<ResponseBody> call = commonCall.doGet(url, parameters);
            syncCall(call, callback);
        }
    }

    public <T> void getFullPath(String fullUrl, Map<String, Object> parameters, HttpCallback<T> callback) {

        if (parameters == null || parameters.isEmpty()) {
            Call<ResponseBody> call = commonCall.doGetFullPath(fullUrl);
            enqueueCall(call, callback);
        } else {
            Call<ResponseBody> call = commonCall.doGetFullPath(fullUrl, parameters);
            enqueueCall(call, callback);
        }
    }

    public void uploadFile(String url, String filePath, String fileDes, boolean isFullUrl, final FileResponseResult callback) {
        final File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        RequestBody description = RequestBody.create(MediaType.parse("multipart/form-data; charset=utf-8"), fileDes);
        RequestBody requestBody = UploadFileRequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        Call<ResponseBody> call = null;
        if (isFullUrl) {
            call = commonCall.uploadFileFullPath(url, description, part);
        } else {
            call = commonCall.uploadFileFullPath(url, description, part);
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String body = response.body().string();
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("-1", response.errorBody().string());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure("-1", t.getMessage());
            }
        });


    }


    public void uploadFiles(String url, List<String> filePathList, boolean isFullUrl, final FileResponseResult callback) {
        int curUploadProgress = 0;
        if (filePathList == null || filePathList.size() == 0) {
            return;
        }
        List<File> fileList = new ArrayList<>();
        long totalSize = 0;
        for (String filePath : filePathList) {
            File file = new File(filePath);
            if (!file.exists()) {
                continue;
            }
            totalSize += file.length();
            fileList.add(file);
        }

        HashMap<String, RequestBody> params = new HashMap<>();
        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);
            RequestBody body =
                    RequestBody.create(MediaType.parse("multipart/form-data"), file);
            params.put("file[]\"; filename=\"" + file.getName(), body);
        }

        Call<ResponseBody> call = null;
        if (isFullUrl) {
            call = commonCall.uploadFilesFullPath(url, params);
        } else {
            call = commonCall.uploadFilesFullPath(url, params);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String body = response.body().string();
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("-1", response.errorBody().string());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure("-1", t.getMessage());
            }
        });
    }


    public <T> void sendJsonRequest(final Call<T> call, String jsonParams, final HttpCallback<T> observer) {
        RequestBody requestBody = createJsonRequestBody(jsonParams);
    }

    public <T> void sendRequest(final Call<T> call, final HttpCallback<T> observer) {
        try {
            if (doInterceptor(call, observer)) {
                return;
            }
            call.enqueue(new HttpCallbackImpl(observer));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> T sendRequestSync(Call<T> call) {
        try {
            if (doInterceptor(call, null)) {
                return null;
            }
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
//                Error error = (Error) RetrofitHelper.getRetrofitInstanse().responseBodyConverter(Error.class, new Annotation[0])
//                        .convert(response.errorBody());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public RequestBody createJsonRequestBody(String jsonString) {
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonString);
    }

    public <T> RequestBody createJsonRequestBody(T t) {
        Gson gson = new Gson();
        String json = gson.toJson(t);
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
    }


    public <T> T create(Class<T> clazz) {
        return getRetrofit().create(clazz);
    }
}
