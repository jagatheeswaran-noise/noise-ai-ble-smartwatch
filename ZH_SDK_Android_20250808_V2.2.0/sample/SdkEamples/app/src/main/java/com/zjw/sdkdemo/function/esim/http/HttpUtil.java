package com.zjw.sdkdemo.function.esim.http;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtil {

    private volatile static HttpUtil instance;
    private HttpResponseListener mHttpResponseListener;

    private final OkHttpClient httpClient;

    private final Logger logger;

    private RetryInterceptor retryInterceptor;

    private Context context;

    public HttpUtil(HttpResponseListener httpResponseListener, Context context) {
        mHttpResponseListener = httpResponseListener;
        logger = Logger.getInstance();
        logger.setDEBUG_FLAG(true);
        context = context.getApplicationContext();

        retryInterceptor = new RetryInterceptor();
        httpClient = getHttpClientWithCertificate(context);
    }

    public static HttpUtil getInstance(HttpResponseListener httpResponseListener, Context context) {
        if (instance == null) {
            synchronized (HttpUtil.class) {
                if (instance == null) {
                    instance = new HttpUtil(httpResponseListener, context);
                }
            }
        }
        return instance;
    }

    private HttpUtil(HttpResponseListener httpResponseListener) {
        mHttpResponseListener = httpResponseListener;
        logger = Logger.getInstance();
        logger.setDEBUG_FLAG(true);

        retryInterceptor = new RetryInterceptor();
        httpClient = new OkHttpClient.Builder()
                .addInterceptor(retryInterceptor)
                .build();

    }

//    public static HttpUtil getInstance(HttpResponseListener httpResponseListener) {
//        if (instance == null) {
//            synchronized (HttpUtil.class) {
//                if (instance == null) {
//                    instance = new HttpUtil(httpResponseListener);
//                }
//            }
//        }
//        return instance;
//    }

    public interface HttpResponseListener {
        void onSuccess(String responseBodyInfo,int code);

        void onFailed(String errorInfo,int code);
    }

    public void callHttpRequest(String uri, String headers, String body, byte control) throws MalformedURLException {
        logger.i("callHttpRequest");
        URL completeUrl = new URL(uri);

        MediaType mediaType = MediaType.Companion.parse(HttpConstants.CONTENT_TYPE_JSON);
        RequestBody requestBody = RequestBody.Companion.create(body, mediaType);

        Request.Builder requestBuilder =
                new Request.Builder()
                        .post(requestBody)
                        .url(completeUrl.toString())
                        .headers(HeadersConverter.stringToHeaders(headers));

        Request request = requestBuilder.build();

        if (control == 0x08){
            httpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mHttpResponseListener.onFailed(e.toString(),404);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Log.e("TAG", "code: "+response.code() );
                    Log.e("TAG", "onResponse: "+response.toString() );
                    //{"header":{"functionExecutionStatus":{"status":"Failed","statusCodeData":{"subjectCode":"8.1.1","reasonCode":"3.8","subjectIdentifier":"89033023426300000000019675655040","message":"The EID is not the same between reservation and request."}}}}

                    switch (response.code()) {
                        case HttpConstants.CODE_OK:
                            if (response.body() != null) {
                                logger.i("Https onResponse");
                                mHttpResponseListener.onSuccess(response.body().string(),response.code());
                            }
                            break;
                        case HttpConstants.CODE_NO_CONTENT:
                            mHttpResponseListener.onSuccess("",response.code());
                            logger.d("response code:" + response.code());
                            break;
                        default:
                            mHttpResponseListener.onFailed("call::Unexpected status code" + response.code(),response.code());
                            logger.e("call::Unexpected status code" + response.code());
                            break;
                    }
                }
            });
        }
    }

    private String getCompleteUrl(String operatorPath, String path) {
        logger.d("getCompleteUrl");
        String completeUrl;
        String baseUrl = checkAndSanitizeBaseUrl(operatorPath);
        logger.d("path:" + path);
        if (path.charAt(0) == '/') {
            completeUrl = baseUrl + path;
        } else {
            completeUrl = baseUrl + "/" + path;
        }
        logger.i("completeUrl:" + completeUrl);
        return completeUrl;
    }

    private String checkAndSanitizeBaseUrl(String operatorPath) {
        logger.d("checkAndSanitizeBaseUrl");

        logger.d("operatorPath:" + operatorPath);

        String protocol = HttpConstants.HTTPS;

        // remove trailing slash
        if (operatorPath.endsWith("/")) {
            return protocol + operatorPath.substring(0, operatorPath.length() - 1);
        } else {
            return protocol + operatorPath;
        }
    }

    private Headers getDefaultHeaders() {
        logger.d("getDefaultHeaders");
        Headers.Builder builder = new Headers.Builder();
        builder.add(HttpConstants.HEADER_CONNECTION, HttpConstants.HEADER_VALUE_KEEP_ALIVE);
        builder.add(HttpConstants.HEADER_PROXY_CONNECTION, HttpConstants.HEADER_VALUE_KEEP_ALIVE);
        builder.add(HttpConstants.HEADER_PROTOCOL, HttpConstants.HEADER_VALUE_PROTOCOL);
        builder.add(HttpConstants.HEADER_USERAGENT, HttpConstants.HEADER_VALUE_USERAGENT);
        return builder.build();
    }

    private OkHttpClient getHttpClientWithoutCertificate() {
        logger.i("getHttpClientWithoutCertificate");
        OkHttpClient.Builder okbul = new OkHttpClient.Builder();
        okbul.addInterceptor(retryInterceptor);
        return okbul.build();
    }

    private OkHttpClient getHttpClientWithCertificate(Context context) {
        logger.i("getHttpClientWithCertificate");
        OkHttpClient.Builder okbul = new OkHttpClient.Builder();
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certInputStream = context.getAssets().open("Symantec_GSMA_RSPv2-Root-CI1.crt");
            Certificate ca;
            try {
                ca = (X509Certificate) cf.generateCertificate(certInputStream);
                logger.i("Certificate:"+ca.toString());
            } finally {
                certInputStream.close();
            }

            // Create the Keystore and import the certificate
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create the TrustManager, which checks whether you trust the server's certificate
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create the SocketFactory required for the TLS connection
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            okbul.addInterceptor(retryInterceptor);
            okbul.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0]);
            return okbul.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private class RetryInterceptor implements Interceptor {
        private static final int MAX_RETRY_COUNT = 3; // Maximum number of retries
        private int retryCount = 0;

        RetryInterceptor() {
        }

        @NonNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            logger.d("intercept");
            Request request = chain.request();
            Response response = null;
            IOException exception = null;

            // Tries to execute the request and tries to retry if it returns an empty value or an exception
            while (response == null && retryCount < MAX_RETRY_COUNT) {
                try {
                    response = chain.proceed(request);
                } catch (IOException e) {
                    exception = e;
                    retryCount++;
                    logger.e("Request error, retryCount=" + retryCount);

                    try {
                        Thread.sleep(1000); // Sleep for 1 second and then try the request again
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }

            if (response == null && exception != null) {
                throw exception;
            }

            return response;
        }
    }
}
