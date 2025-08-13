package com.zjw.sdkdemo.function.esim.http;

public class HttpConstants {

    public static final String HTTPS = "https://";
    public static String THALES = "thales1-livelab.prod.ondemandconnectivity.com";

    public static final int CODE_OK = 200;
    public static final int CODE_NO_CONTENT = 204;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_SERVER_ERROR = 500;

    public static final String PATH_PREFIX = "/gsma/rsp2/es9plus";

    public static final String PATH_INITIATE_AUTHENTICATION = "/initiateAuthentication";
    public static final String PATH_AUTHENTICATE_CLIENT = "/authenticateClient";
    public static final String PATH_GET_BOUND_PROFILE_PACKAGE="/getBoundProfilePackage";
    public static final String PATH_HANDLE_NOTIFICATION = "/handleNotification";
    public static final String PATH_CANCEL_SESSION="/cancelSession";

    public static final String HEADER_CONNECTION = "connection";
    public static final String HEADER_PROXY_CONNECTION = "proxy-connection";
    public static final String HEADER_VALUE_KEEP_ALIVE = "Keep-Alive";
    public static final String HEADER_PROTOCOL = "X-Admin-Protocol";
    public static final String HEADER_VALUE_PROTOCOL = "gsma/rsp/v2.3.0";
    public static final String HEADER_USERAGENT = "User-Agent";
    public static final String HEADER_VALUE_USERAGENT = "gsma-rsp-lpad";
    public static final String CONTENT_TYPE_JSON = "application/json";


    private HttpConstants() {
    }
}
