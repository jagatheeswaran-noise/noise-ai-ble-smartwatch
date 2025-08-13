package com.zjw.sdkdemo.function.esim.http;

import okhttp3.Headers;

public class HeadersConverter {
    /**
     * Convert the Headers type variable to a string
     */
    public static String headersToString(Headers headers) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String name : headers.names()) {
            stringBuilder.append(name).append(": ").append(headers.get(name)).append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Converts the string to a Headers variable
     */
    public static Headers stringToHeaders(String headersString) {
        Headers.Builder builder = new Headers.Builder();
        String[] headers = headersString.split("\n");
        for (String header : headers) {
            String[] parts = header.split(": ");
            if (parts.length > 1) {
                builder.add(parts[0], parts[1]);
            }
        }
        return builder.build();
    }
}
