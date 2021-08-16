/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.net.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import jdk.internal.net.http.Http1Exchange.Http1BodySubscriber;
import jdk.internal.net.http.common.HttpHeadersBuilder;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.Utils;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 *  An HTTP/1.1 request.
 */
class Http1Request {

    private static final String COOKIE_HEADER = "Cookie";
    private static final BiPredicate<String,String> NOCOOKIES =
            (k,v) -> !COOKIE_HEADER.equalsIgnoreCase(k);

    private final HttpRequestImpl request;
    private final Http1Exchange<?> http1Exchange;
    private final HttpConnection connection;
    private final HttpRequest.BodyPublisher requestPublisher;
    private volatile HttpHeaders userHeaders;
    private final HttpHeadersBuilder systemHeadersBuilder;
    private volatile boolean streaming;
    private volatile long contentLength;

    Http1Request(HttpRequestImpl request,
                 Http1Exchange<?> http1Exchange)
        throws IOException
    {
        this.request = request;
        this.http1Exchange = http1Exchange;
        this.connection = http1Exchange.connection();
        this.requestPublisher = request.requestPublisher;  // may be null
        this.userHeaders = request.getUserHeaders();
        this.systemHeadersBuilder = request.getSystemHeadersBuilder();
    }

    private void logHeaders(String completeHeaders) {
        if (Log.headers()) {
            //StringBuilder sb = new StringBuilder(256);
            //sb.append("REQUEST HEADERS:\n");
            //Log.dumpHeaders(sb, "    ", systemHeaders);
            //Log.dumpHeaders(sb, "    ", userHeaders);
            //Log.logHeaders(sb.toString());

            String s = completeHeaders.replaceAll("\r\n", "\n");
            if (s.endsWith("\n\n")) s = s.substring(0, s.length() - 2);
            Log.logHeaders("REQUEST HEADERS:\n{0}\n", s);
        }
    }


    public void collectHeaders0(StringBuilder sb) {
        BiPredicate<String,String> filter =
                connection.headerFilter(request);

        // Filter out 'Cookie:' headers, we will collect them at the end.
        BiPredicate<String,String> nocookies = NOCOOKIES.and(filter);

        HttpHeaders systemHeaders = systemHeadersBuilder.build();
        HttpClient client = http1Exchange.client();

        // Filter overridable headers from userHeaders
        userHeaders = HttpHeaders.of(userHeaders.map(),
                      connection.contextRestricted(request, client));

        final HttpHeaders uh = userHeaders;

        // Filter any headers from systemHeaders that are set in userHeaders
        systemHeaders = HttpHeaders.of(systemHeaders.map(), (k,v) -> uh.firstValue(k).isEmpty());

        // If we're sending this request through a tunnel,
        // then don't send any preemptive proxy-* headers that
        // the authentication filter may have saved in its
        // cache.
        collectHeaders1(sb, systemHeaders, nocookies);

        // If we're sending this request through a tunnel,
        // don't send any user-supplied proxy-* headers
        // to the target server.
        collectHeaders1(sb, userHeaders, nocookies);

        // Gather all 'Cookie:' headers and concatenate their
        // values in a single line.
        collectCookies(sb, systemHeaders, userHeaders);

        // terminate headers
        sb.append('\r').append('\n');
    }

    // Concatenate any 'Cookie:' header in a single line, as mandated
    // by RFC 6265, section 5.4:
    //
    // <<When the user agent generates an HTTP request, the user agent MUST
    //   NOT attach more than one Cookie header field.>>
    //
    // This constraint is relaxed for the HTTP/2 protocol, which
    // explicitly allows sending multiple Cookie header fields.
    // RFC 7540 section 8.1.2.5:
    //
    // <<To allow for better compression efficiency, the Cookie header
    //   field MAY be split into separate header fields, each with one or
    //   more cookie-pairs.>>
    //
    // This method will therefore concatenate multiple Cookie header field
    // values into a single field, in a similar way than was implemented in
    // the legacy HttpURLConnection.
    //
    // Note that at this point this method performs no further validation
    // on the actual field-values, except to check that they do not contain
    // any illegal character for header field values.
    //
    private void collectCookies(StringBuilder sb,
                                HttpHeaders system,
                                HttpHeaders user) {
        List<String> systemList = system.allValues(COOKIE_HEADER);
        List<String> userList = user.allValues(COOKIE_HEADER);
        boolean found = false;
        if (systemList != null) {
            for (String cookie : systemList) {
                if (!found) {
                    found = true;
                    sb.append(COOKIE_HEADER).append(':').append(' ');
                } else {
                    sb.append(';').append(' ');
                }
                sb.append(cookie);
            }
        }
        if (userList != null) {
            for (String cookie : userList) {
                if (!found) {
                    found = true;
                    sb.append(COOKIE_HEADER).append(':').append(' ');
                } else {
                    sb.append(';').append(' ');
                }
                sb.append(cookie);
            }
        }
        if (found) sb.append('\r').append('\n');
    }

    private void collectHeaders1(StringBuilder sb,
                                 HttpHeaders headers,
                                 BiPredicate<String,String> filter) {
        for (Map.Entry<String,List<String>> entry : headers.map().entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                if (!filter.test(key, value))
                    continue;
                sb.append(key).append(':').append(' ')
                        .append(value)
                        .append('\r').append('\n');
            }
        }
    }

    private String getPathAndQuery(URI uri) {
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (query == null) {
            query = "";
        }
        if (query.isEmpty()) {
            return Utils.encode(path);
        } else {
            return Utils.encode(path + "?" + query);
        }
    }

    private String authorityString(InetSocketAddress addr) {
        return addr.getHostString() + ":" + addr.getPort();
    }

    private String hostString() {
        URI uri = request.uri();
        int port = uri.getPort();
        String host = uri.getHost();

        boolean defaultPort;
        if (port == -1) {
            defaultPort = true;
        } else if (request.secure()) {
            defaultPort = port == 443;
        } else {
            defaultPort = port == 80;
        }

        if (defaultPort) {
            return host;
        } else {
            return host + ":" + Integer.toString(port);
        }
    }

    private String requestURI() {
        URI uri = request.uri();
        String method = request.method();

        if ((request.proxy() == null && !method.equals("CONNECT"))
                || request.isWebSocket()) {
            return getPathAndQuery(uri);
        }
        if (request.secure()) {
            if (request.method().equals("CONNECT")) {
                // use authority for connect itself
                return authorityString(request.authority());
            } else {
                // requests over tunnel do not require full URL
                return getPathAndQuery(uri);
            }
        }
        if (request.method().equals("CONNECT")) {
            // use authority for connect itself
            return authorityString(request.authority());
        }

        return uri == null? authorityString(request.authority()) : uri.toString();
    }

    private boolean finished;

    synchronized boolean finished() {
        return  finished;
    }

    synchronized void setFinished() {
        finished = true;
    }

    List<ByteBuffer> headers() {
        if (Log.requests() && request != null) {
            Log.logRequest(request.toString());
        }
        String uriString = requestURI();
        StringBuilder sb = new StringBuilder(64);
        sb.append(request.method())
          .append(' ')
          .append(uriString)
          .append(" HTTP/1.1\r\n");

        URI uri = request.uri();
        if (uri != null) {
            systemHeadersBuilder.setHeader("Host", hostString());
        }
        if (requestPublisher == null) {
            // Not a user request, or maybe a method, e.g. GET, with no body.
            contentLength = 0;
        } else {
            contentLength = requestPublisher.contentLength();
        }

        if (contentLength == 0) {
            systemHeadersBuilder.setHeader("Content-Length", "0");
        } else if (contentLength > 0) {
            systemHeadersBuilder.setHeader("Content-Length", Long.toString(contentLength));
            streaming = false;
        } else {
            streaming = true;
            systemHeadersBuilder.setHeader("Transfer-encoding", "chunked");
        }
        collectHeaders0(sb);
        String hs = sb.toString();
        logHeaders(hs);
        ByteBuffer b = ByteBuffer.wrap(hs.getBytes(US_ASCII));
        return List.of(b);
    }

    Http1BodySubscriber continueRequest()  {
        Http1BodySubscriber subscriber;
        if (streaming) {
            subscriber = new StreamSubscriber();
            requestPublisher.subscribe(subscriber);
        } else {
            if (contentLength == 0)
                return null;

            subscriber = new FixedContentSubscriber();
            requestPublisher.subscribe(subscriber);
        }
        return subscriber;
    }

    final class StreamSubscriber extends Http1BodySubscriber {

        StreamSubscriber() { super(debug); }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (isSubscribed()) {
                Throwable t = new IllegalStateException("already subscribed");
                http1Exchange.appendToOutgoing(t);
            } else {
                setSubscription(subscription);
            }
        }

        @Override
        public void onNext(ByteBuffer item) {
            Objects.requireNonNull(item);
            if (complete) {
                Throwable t = new IllegalStateException("subscription already completed");
                http1Exchange.appendToOutgoing(t);
            } else {
                int chunklen = item.remaining();
                ArrayList<ByteBuffer> l = new ArrayList<>(3);
                l.add(getHeader(chunklen));
                l.add(item);
                l.add(ByteBuffer.wrap(CRLF));
                http1Exchange.appendToOutgoing(l);
            }
        }

        @Override
        public String currentStateMessage() {
            return "streaming request body " + (complete ? "complete" : "incomplete");
        }

        @Override
        public void onError(Throwable throwable) {
            if (complete)
                return;

            cancelSubscription();
            http1Exchange.appendToOutgoing(throwable);
        }

        @Override
        public void onComplete() {
            if (complete) {
                Throwable t = new IllegalStateException("subscription already completed");
                http1Exchange.appendToOutgoing(t);
            } else {
                ArrayList<ByteBuffer> l = new ArrayList<>(2);
                l.add(ByteBuffer.wrap(EMPTY_CHUNK_BYTES));
                l.add(ByteBuffer.wrap(CRLF));
                complete = true;
                //setFinished();
                http1Exchange.appendToOutgoing(l);
                http1Exchange.appendToOutgoing(COMPLETED);
                setFinished();  // TODO: before or after,? does it matter?

            }
        }
    }

    final class FixedContentSubscriber extends Http1BodySubscriber {

        private volatile long contentWritten;
        FixedContentSubscriber() { super(debug); }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (isSubscribed()) {
                Throwable t = new IllegalStateException("already subscribed");
                http1Exchange.appendToOutgoing(t);
            } else {
                setSubscription(subscription);
            }
        }

        @Override
        public void onNext(ByteBuffer item) {
            if (debug.on()) debug.log("onNext");
            Objects.requireNonNull(item);
            if (complete) {
                Throwable t = new IllegalStateException("subscription already completed");
                http1Exchange.appendToOutgoing(t);
            } else {
                long writing = item.remaining();
                long written = (contentWritten += writing);

                if (written > contentLength) {
                    cancelSubscription();
                    String msg = connection.getConnectionFlow()
                                  + " [" + Thread.currentThread().getName() +"] "
                                  + "Too many bytes in request body. Expected: "
                                  + contentLength + ", got: " + written;
                    http1Exchange.appendToOutgoing(new IOException(msg));
                } else {
                    http1Exchange.appendToOutgoing(List.of(item));
                }
            }
        }

        @Override
        public String currentStateMessage() {
            return format("fixed content-length: %d, bytes sent: %d",
                           contentLength, contentWritten);
        }

        @Override
        public void onError(Throwable throwable) {
            if (debug.on()) debug.log("onError");
            if (complete)  // TODO: error?
                return;

            cancelSubscription();
            http1Exchange.appendToOutgoing(throwable);
        }

        @Override
        public void onComplete() {
            if (debug.on()) debug.log("onComplete");
            if (complete) {
                Throwable t = new IllegalStateException("subscription already completed");
                http1Exchange.appendToOutgoing(t);
            } else {
                complete = true;
                long written = contentWritten;
                if (contentLength > written) {
                    cancelSubscription();
                    Throwable t = new IOException(connection.getConnectionFlow()
                                         + " [" + Thread.currentThread().getName() +"] "
                                         + "Too few bytes returned by the publisher ("
                                                  + written + "/"
                                                  + contentLength + ")");
                    http1Exchange.appendToOutgoing(t);
                } else {
                    http1Exchange.appendToOutgoing(COMPLETED);
                }
            }
        }
    }

    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] EMPTY_CHUNK_BYTES = {'0', '\r', '\n'};

    /** Returns a header for a particular chunk size */
    private static ByteBuffer getHeader(int size) {
        String hexStr = Integer.toHexString(size);
        byte[] hexBytes = hexStr.getBytes(US_ASCII);
        byte[] header = new byte[hexStr.length()+2];
        System.arraycopy(hexBytes, 0, header, 0, hexBytes.length);
        header[hexBytes.length] = CRLF[0];
        header[hexBytes.length+1] = CRLF[1];
        return ByteBuffer.wrap(header);
    }

    final Logger debug = Utils.getDebugLogger(this::toString, Utils.DEBUG);

}
