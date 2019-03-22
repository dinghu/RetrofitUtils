package com.retrofit.utils.interceptor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Platform;
import okio.Buffer;
import okio.BufferedSource;

public final class HttpLoggingInterceptor implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public interface Logger {
        void log(String message);

        HttpLoggingInterceptor.Logger DEFAULT = new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Platform.get().log(Level.INFO.intValue(), message, new Throwable(message));
            }
        };
    }

    private boolean logRequetBase;
    private boolean logRequetHeader;
    private boolean logRequetBody;
    private boolean logResponseBase;
    private boolean logResponseHeader;
    private boolean logResponseBody;

    private void reset() {
        logRequetBase = true;
        logRequetHeader = false;
        logRequetBody = false;
        logResponseBase = true;
        logResponseHeader = false;
        logResponseBody = false;
    }

    public HttpLoggingInterceptor() {
        this(HttpLoggingInterceptor.Logger.DEFAULT);
    }

    public HttpLoggingInterceptor(HttpLoggingInterceptor.Logger logger) {
        this.logger = logger;
        reset();
    }

    private final HttpLoggingInterceptor.Logger logger;

    private void logRequestBase(Chain chain, Request request) throws IOException {
        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        String requestStartMessage = "--> " + request.method() + ' ' + request.url() + ' ' + protocol(protocol);
        logger.log(requestStartMessage);
    }

    private void logHeader(Request request, RequestBody requestBody) throws IOException {
        boolean hasRequestBody = requestBody != null;
        if (hasRequestBody) {
            // Request body headers are only present when installed as a network interceptor. Force
            // them to be included (when available) so there values are known.
            if (requestBody.contentType() != null) {
                logger.log("Content-Type: " + requestBody.contentType());
            }
            if (requestBody.contentLength() != -1) {
                logger.log("Content-Length: " + requestBody.contentLength());
            }
        }

        Headers headers = request.headers();
        for (int i = 0, count = headers.size(); i < count; i++) {
            String name = headers.name(i);
            // Skip headers from the request body as they are explicitly logged above.
            if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                logger.log(name + ": " + headers.value(i));
            }
        }

        if (!logRequetBody || !hasRequestBody) {
            logger.log("--> END " + request.method());
        } else if (bodyEncoded(request.headers())) {
            logger.log("--> END " + request.method() + " (encoded body omitted)");
        } else {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);

            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                contentType.charset(UTF8);
            }

            logger.log("");
            logger.log(buffer.readString(charset));

            logger.log("--> END " + request.method()
                    + " (" + requestBody.contentLength() + "-byte body)");
        }
    }

    private void logRequestBody(Request request, RequestBody requestBody) throws IOException {
        if (requestBody == null || bodyEncoded(request.headers())) {
            logger.log("<-- END " + request.method() + " (encoded body omitted)");
        } else {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);

            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }

            if (requestBody.contentLength() != 0) {
                logger.log("");
                logger.log(buffer.clone().readString(charset));
            }
            logger.log("--> END " + request.method()
                    + " (" + requestBody.contentLength() + "-byte body)");
        }
    }

    private void logResponseBase(Response response, long tookMs) throws IOException {
        logger.log("<-- " + protocol(response.protocol()) + ' ' + response.code() + ' ' + response.message()
                + " (" + tookMs + "ms)')");
    }

    private void logResponseHeader(Response response) throws IOException {
        Headers headers = response.headers();
        for (int i = 0, count = headers.size(); i < count; i++) {
            logger.log(headers.name(i) + ": " + headers.value(i));
        }
    }

    private void logBody(Response response, ResponseBody responseBody) throws IOException {
        if (bodyEncoded(response.headers())) {
            logger.log("<-- END HTTP (encoded body omitted)");
        } else {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire body.
            Buffer buffer = source.buffer();

            Charset charset = UTF8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }

            if (responseBody.contentLength() != 0) {
                logger.log("");
                logger.log(buffer.clone().readString(charset));
            }

            logger.log("<-- END HTTP (" + buffer.size() + "-byte body)");
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        RequestBody requestBody = request.body();

        if (logRequetBase) {
            logRequestBase(chain, request);
        }

        if (logRequetHeader) {
            logHeader(request, requestBody);
        }

        if (logRequetBody) {
            logRequestBody(request, requestBody);
        }

        long startNs = System.nanoTime();
        Response response = chain.proceed(request);
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();

        if (logResponseBase) {
            logResponseBase(response, tookMs);
        }

        if (logResponseHeader) {
            logResponseHeader(response);
        }

        if (logResponseBody) {
            logBody(response, responseBody);
        }

        return response;
    }

//    @Override public Response intercept(Chain chain) throws IOException {
//        HttpLoggingInterceptor.Level level = this.level;
//
//        Request request = chain.request();
//        if (level == HttpLoggingInterceptor.Level.NONE) {
//            return chain.proceed(request);
//        }
//
//        boolean logBody = level == HttpLoggingInterceptor.Level.BODY;
//        boolean logHeaders = logBody || level == HttpLoggingInterceptor.Level.HEADERS;
//
//        RequestBody requestBody = request.body();
//        boolean hasRequestBody = requestBody != null;
//
//        Connection connection = chain.connection();
//        Protocol protocol = connection != null ? connection.getProtocol() : Protocol.HTTP_1_1;
//        String requestStartMessage =
//                "--> " + request.method() + ' ' + request.httpUrl() + ' ' + protocol(protocol);
//        if (!logHeaders && hasRequestBody) {
//            requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
//        }
//        logger.log(requestStartMessage);
//
//        if (logHeaders) {
//            if (hasRequestBody) {
//                // Request body headers are only present when installed as a network interceptor. Force
//                // them to be included (when available) so there values are known.
//                if (requestBody.contentType() != null) {
//                    logger.log("Content-Type: " + requestBody.contentType());
//                }
//                if (requestBody.contentLength() != -1) {
//                    logger.log("Content-Length: " + requestBody.contentLength());
//                }
//            }
//
//            Headers headers = request.headers();
//            for (int i = 0, count = headers.size(); i < count; i++) {
//                String name = headers.name(i);
//                // Skip headers from the request body as they are explicitly logged above.
//                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
//                    logger.log(name + ": " + headers.value(i));
//                }
//            }
//
//            if (!logBody || !hasRequestBody) {
//                logger.log("--> END " + request.method());
//            } else if (bodyEncoded(request.headers())) {
//                logger.log("--> END " + request.method() + " (encoded body omitted)");
//            } else {
//                Buffer buffer = new Buffer();
//                requestBody.writeTo(buffer);
//
//                Charset charset = UTF8;
//                MediaType contentType = requestBody.contentType();
//                if (contentType != null) {
//                    contentType.charset(UTF8);
//                }
//
//                logger.log("");
//                logger.log(buffer.readString(charset));
//
//                logger.log("--> END " + request.method()
//                        + " (" + requestBody.contentLength() + "-byte body)");
//            }
//        }
//
//        long startNs = System.nanoTime();
//        Response response = chain.proceed(request);
//        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
//
//        ResponseBody responseBody = response.body();
//        logger.log("<-- " + protocol(response.protocol()) + ' ' + response.code() + ' '
//                + response.message() + " (" + tookMs + "ms"
//                + (!logHeaders ? ", " + responseBody.contentLength() + "-byte body" : "") + ')');
//
//        if (logHeaders) {
//            Headers headers = response.headers();
//            for (int i = 0, count = headers.size(); i < count; i++) {
//                logger.log(headers.name(i) + ": " + headers.value(i));
//            }
//
//            if (!logBody || !HttpEngine.hasBody(response)) {
//                logger.log("<-- END HTTP");
//            } else if (bodyEncoded(response.headers())) {
//                logger.log("<-- END HTTP (encoded body omitted)");
//            } else {
//                BufferedSource source = responseBody.source();
//                source.request(Long.MAX_VALUE); // Buffer the entire body.
//                Buffer buffer = source.buffer();
//
//                Charset charset = UTF8;
//                MediaType contentType = responseBody.contentType();
//                if (contentType != null) {
//                    charset = contentType.charset(UTF8);
//                }
//
//                if (responseBody.contentLength() != 0) {
//                    logger.log("");
//                    logger.log(buffer.clone().readString(charset));
//                }
//
//                logger.log("<-- END HTTP (" + buffer.size() + "-byte body)");
//            }
//        }
//
//        return response;
//    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }

    private static String protocol(Protocol protocol) {
        return protocol == Protocol.HTTP_1_0 ? "HTTP/1.0" : "HTTP/1.1";
    }

    public boolean isLogRequetBase() {
        return logRequetBase;
    }

    public void setLogRequetBase(boolean logRequetBase) {
        this.logRequetBase = logRequetBase;
    }

    public boolean isLogRequetHeader() {
        return logRequetHeader;
    }

    public void setLogRequetHeader(boolean logRequetHeader) {
        this.logRequetHeader = logRequetHeader;
    }

    public boolean isLogRequetBody() {
        return logRequetBody;
    }

    public void setLogRequetBody(boolean logRequetBody) {
        this.logRequetBody = logRequetBody;
    }

    public boolean isLogResponseBase() {
        return logResponseBase;
    }

    public void setLogResponseBase(boolean logResponseBase) {
        this.logResponseBase = logResponseBase;
    }

    public boolean isLogResponseHeader() {
        return logResponseHeader;
    }

    public void setLogResponseHeader(boolean logResponseHeader) {
        this.logResponseHeader = logResponseHeader;
    }

    public boolean isLogResponseBody() {
        return logResponseBody;
    }

    public void setLogResponseBody(boolean logResponseBody) {
        this.logResponseBody = logResponseBody;
    }
}