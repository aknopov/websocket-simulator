package com.aknopov.wssimulator;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

/**
 * WebSocket protocol upgrade info
 *
 * @param requestUri request URI from client
 * @param queryString request query string
 * @param reqHeaders request headers
 * @param respHeaders response headers
 * @param status WebSocket response status code (HTTP-101, HTTP-404 etc)
 */
@SuppressWarnings("ImmutableMemberCollection")
public record ProtocolUpgrade(URI requestUri, String queryString, Map<String, List<String>> reqHeaders,
                              Map<String, List<String>> respHeaders, int status)
{
    public static final int SWITCH_SUCCESS_CODE = 101;

    // Read-only headers for client side
    public ProtocolUpgrade(URI requestUri, String queryString, HttpHeaders reqHeaders, HttpHeaders respHeaders, int status) {
        this(requestUri, queryString, reqHeaders.map(), respHeaders.map(), status);
    }
}
