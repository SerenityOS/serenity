/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.common;

import sun.net.NetProperties;
import sun.net.util.IPAddressUtil;
import sun.net.www.HeaderParser;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLPermission;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.stream.Collectors.joining;
import jdk.internal.net.http.HttpRequestImpl;

/**
 * Miscellaneous utilities
 */
public final class Utils {

    public static final boolean ASSERTIONSENABLED;

    static {
        boolean enabled = false;
        assert enabled = true;
        ASSERTIONSENABLED = enabled;
    }

//    public static final boolean TESTING;
//    static {
//        if (ASSERTIONSENABLED) {
//            PrivilegedAction<String> action = () -> System.getProperty("test.src");
//            TESTING = AccessController.doPrivileged(action) != null;
//        } else TESTING = false;
//    }
    public static final boolean DEBUG = // Revisit: temporary dev flag.
            getBooleanProperty(DebugLogger.HTTP_NAME, false);
    public static final boolean DEBUG_WS = // Revisit: temporary dev flag.
            getBooleanProperty(DebugLogger.WS_NAME, false);
    public static final boolean DEBUG_HPACK = // Revisit: temporary dev flag.
            getBooleanProperty(DebugLogger.HPACK_NAME, false);
    public static final boolean TESTING = DEBUG;

    public static final boolean isHostnameVerificationDisabled = // enabled by default
            hostnameVerificationDisabledValue();

    private static boolean hostnameVerificationDisabledValue() {
        String prop = getProperty("jdk.internal.httpclient.disableHostnameVerification");
        if (prop == null)
            return false;
        return prop.isEmpty() ? true : Boolean.parseBoolean(prop);
    }

    /**
     * Allocated buffer size. Must never be higher than 16K. But can be lower
     * if smaller allocation units preferred. HTTP/2 mandates that all
     * implementations support frame payloads of at least 16K.
     */
    private static final int DEFAULT_BUFSIZE = 16 * 1024;

    public static final int BUFSIZE = getIntegerNetProperty(
            "jdk.httpclient.bufsize", DEFAULT_BUFSIZE
    );

    public static final BiPredicate<String,String> ACCEPT_ALL = (x,y) -> true;

    private static final Set<String> DISALLOWED_HEADERS_SET = getDisallowedHeaders();

    private static Set<String> getDisallowedHeaders() {
        Set<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.addAll(Set.of("connection", "content-length", "expect", "host", "upgrade"));

        String v = getNetProperty("jdk.httpclient.allowRestrictedHeaders");
        if (v != null) {
            // any headers found are removed from set.
            String[] tokens = v.trim().split(",");
            for (String token : tokens) {
                headers.remove(token);
            }
            return Collections.unmodifiableSet(headers);
        } else {
            return Collections.unmodifiableSet(headers);
        }
    }

    public static final BiPredicate<String, String>
            ALLOWED_HEADERS = (header, unused) -> !DISALLOWED_HEADERS_SET.contains(header);

    public static final BiPredicate<String, String> VALIDATE_USER_HEADER =
            (name, value) -> {
                assert name != null : "null header name";
                assert value != null : "null header value";
                if (!isValidName(name)) {
                    throw newIAE("invalid header name: \"%s\"", name);
                }
                if (!Utils.ALLOWED_HEADERS.test(name, null)) {
                    throw newIAE("restricted header name: \"%s\"", name);
                }
                if (!isValidValue(value)) {
                    throw newIAE("invalid header value for %s: \"%s\"", name, value);
                }
                return true;
            };

    // Headers that are not generally restricted, and can therefore be set by users,
    // but can in some contexts be overridden by the implementation.
    // Currently, only contains "Authorization" which will
    // be overridden, when an Authenticator is set on the HttpClient.
    // Needs to be BiPred<String,String> to fit with general form of predicates
    // used by caller.

    public static final BiPredicate<String, String> CONTEXT_RESTRICTED(HttpClient client) {
        return (k, v) -> !client.authenticator().isPresent() ||
                (!k.equalsIgnoreCase("Authorization")
                        && !k.equalsIgnoreCase("Proxy-Authorization"));
    }

    public record ProxyHeaders(HttpHeaders userHeaders, HttpHeaders systemHeaders) {}

    private static final BiPredicate<String, String> HOST_RESTRICTED = (k,v) -> !"host".equalsIgnoreCase(k);
    public static final BiPredicate<String, String> PROXY_TUNNEL_RESTRICTED(HttpClient client)  {
        return CONTEXT_RESTRICTED(client).and(HOST_RESTRICTED);
    }

    private static final Predicate<String> IS_HOST = "host"::equalsIgnoreCase;
    private static final Predicate<String> IS_PROXY_HEADER = (k) ->
            k != null && k.length() > 6 && "proxy-".equalsIgnoreCase(k.substring(0,6));
    private static final Predicate<String> NO_PROXY_HEADER =
            IS_PROXY_HEADER.negate();
    private static final Predicate<String> ALL_HEADERS = (s) -> true;

    private static final Set<String> PROXY_AUTH_DISABLED_SCHEMES;
    private static final Set<String> PROXY_AUTH_TUNNEL_DISABLED_SCHEMES;
    static {
        String proxyAuthDisabled =
                getNetProperty("jdk.http.auth.proxying.disabledSchemes");
        String proxyAuthTunnelDisabled =
                getNetProperty("jdk.http.auth.tunneling.disabledSchemes");
        PROXY_AUTH_DISABLED_SCHEMES =
                proxyAuthDisabled == null ? Set.of() :
                        Stream.of(proxyAuthDisabled.split(","))
                                .map(String::trim)
                                .filter((s) -> !s.isEmpty())
                                .collect(Collectors.toUnmodifiableSet());
        PROXY_AUTH_TUNNEL_DISABLED_SCHEMES =
                proxyAuthTunnelDisabled == null ? Set.of() :
                        Stream.of(proxyAuthTunnelDisabled.split(","))
                                .map(String::trim)
                                .filter((s) -> !s.isEmpty())
                                .collect(Collectors.toUnmodifiableSet());
    }

    public static <T> CompletableFuture<T> wrapForDebug(Logger logger, String name, CompletableFuture<T> cf) {
        if (logger.on()) {
            return cf.handle((r,t) -> {
                logger.log("%s completed %s", name, t == null ? "successfully" : t );
                return cf;
            }).thenCompose(Function.identity());
        } else {
            return cf;
        }
    }

    private static final String WSPACES = " \t\r\n";
    private static final boolean isAllowedForProxy(String name,
                                                   String value,
                                                   Set<String> disabledSchemes,
                                                   Predicate<String> allowedKeys) {
        if (!allowedKeys.test(name)) return false;
        if (disabledSchemes.isEmpty()) return true;
        if (name.equalsIgnoreCase("proxy-authorization")) {
            if (value.isEmpty()) return false;
            for (String scheme : disabledSchemes) {
                int slen = scheme.length();
                int vlen = value.length();
                if (vlen == slen) {
                    if (value.equalsIgnoreCase(scheme)) {
                        return false;
                    }
                } else if (vlen > slen) {
                    if (value.substring(0,slen).equalsIgnoreCase(scheme)) {
                        int c = value.codePointAt(slen);
                        if (WSPACES.indexOf(c) > -1
                                || Character.isSpaceChar(c)
                                || Character.isWhitespace(c)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static final BiPredicate<String, String> PROXY_TUNNEL_FILTER =
            (s,v) -> isAllowedForProxy(s, v, PROXY_AUTH_TUNNEL_DISABLED_SCHEMES,
                    // Allows Proxy-* and Host headers when establishing the tunnel.
                    IS_PROXY_HEADER.or(IS_HOST));
    public static final BiPredicate<String, String> PROXY_FILTER =
            (s,v) -> isAllowedForProxy(s, v, PROXY_AUTH_DISABLED_SCHEMES,
                    ALL_HEADERS);
    public static final BiPredicate<String, String> NO_PROXY_HEADERS_FILTER =
            (n,v) -> Utils.NO_PROXY_HEADER.test(n);


    public static boolean proxyHasDisabledSchemes(boolean tunnel) {
        return tunnel ? ! PROXY_AUTH_TUNNEL_DISABLED_SCHEMES.isEmpty()
                      : ! PROXY_AUTH_DISABLED_SCHEMES.isEmpty();
    }

    // WebSocket connection Upgrade headers
    private static final String HEADER_CONNECTION = "Connection";
    private static final String HEADER_UPGRADE    = "Upgrade";

    public static final void setWebSocketUpgradeHeaders(HttpRequestImpl request) {
        request.setSystemHeader(HEADER_UPGRADE, "websocket");
        request.setSystemHeader(HEADER_CONNECTION, "Upgrade");
    }

    public static IllegalArgumentException newIAE(String message, Object... args) {
        return new IllegalArgumentException(format(message, args));
    }
    public static ByteBuffer getBuffer() {
        return ByteBuffer.allocate(BUFSIZE);
    }

    public static Throwable getCompletionCause(Throwable x) {
        Throwable cause = x;
        while ((cause instanceof CompletionException)
                || (cause instanceof ExecutionException)) {
            cause = cause.getCause();
        }
        if (cause == null && cause != x) {
            throw new InternalError("Unexpected null cause", x);
        }
        return cause;
    }

    public static Throwable getCancelCause(Throwable x) {
        Throwable cause = getCompletionCause(x);
        if (cause instanceof ConnectionExpiredException) {
            cause = cause.getCause();
        }
        return cause;
    }

    public static IOException getIOException(Throwable t) {
        if (t instanceof IOException) {
            return (IOException) t;
        }
        Throwable cause = t.getCause();
        if (cause != null) {
            return getIOException(cause);
        }
        return new IOException(t);
    }

    /**
     * Adds a more specific exception detail message, based on the given
     * exception type and the message supplier. This is primarily to present
     * more descriptive messages in IOExceptions that may be visible to calling
     * code.
     *
     * @return a possibly new exception that has as its detail message, the
     *         message from the messageSupplier, and the given throwable as its
     *         cause. Otherwise returns the given throwable
     */
    public static Throwable wrapWithExtraDetail(Throwable t,
                                                Supplier<String> messageSupplier) {
        if (!(t instanceof IOException))
            return t;

        if (t instanceof SSLHandshakeException)
            return t;  // no need to decorate

        String msg = messageSupplier.get();
        if (msg == null)
            return t;

        if (t instanceof ConnectionExpiredException) {
            if (t.getCause() instanceof SSLHandshakeException)
                return t;  // no need to decorate
            IOException ioe = new IOException(msg, t.getCause());
            t = new ConnectionExpiredException(ioe);
        } else {
            IOException ioe = new IOException(msg, t);
            t = ioe;
        }
        return t;
    }

    private Utils() { }

    /**
     * Returns the security permissions required to connect to the proxy, or
     * {@code null} if none is required or applicable.
     */
    public static URLPermission permissionForProxy(InetSocketAddress proxyAddress) {
        if (proxyAddress == null)
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append("socket://")
          .append(proxyAddress.getHostString()).append(":")
          .append(proxyAddress.getPort());
        String urlString = sb.toString();
        return new URLPermission(urlString, "CONNECT");
    }

    /**
     * Returns the security permission required for the given details.
     */
    public static URLPermission permissionForServer(URI uri,
                                                    String method,
                                                    Stream<String> headers) {
        String urlString = new StringBuilder()
                .append(uri.getScheme()).append("://")
                .append(uri.getRawAuthority())
                .append(uri.getRawPath()).toString();

        StringBuilder actionStringBuilder = new StringBuilder(method);
        String collected = headers.collect(joining(","));
        if (!collected.isEmpty()) {
            actionStringBuilder.append(":").append(collected);
        }
        return new URLPermission(urlString, actionStringBuilder.toString());
    }


    // ABNF primitives defined in RFC 7230
    private static final boolean[] tchar      = new boolean[256];
    private static final boolean[] fieldvchar = new boolean[256];

    static {
        char[] allowedTokenChars =
                ("!#$%&'*+-.^_`|~0123456789" +
                 "abcdefghijklmnopqrstuvwxyz" +
                 "ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();
        for (char c : allowedTokenChars) {
            tchar[c] = true;
        }
        for (char c = 0x21; c <= 0xFF; c++) {
            fieldvchar[c] = true;
        }
        fieldvchar[0x7F] = false; // a little hole (DEL) in the range
    }

    /*
     * Validates a RFC 7230 field-name.
     */
    public static boolean isValidName(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c > 255 || !tchar[c]) {
                return false;
            }
        }
        return !token.isEmpty();
    }

    public static class ServerName {
        ServerName(String name, boolean isLiteral) {
            this.name = name;
            this.isLiteral = isLiteral;
        }

        final String name;
        final boolean isLiteral;

        public String getName() {
            return name;
        }

        public boolean isLiteral() {
            return isLiteral;
        }
    }

    /**
     * Analyse the given address and determine if it is literal or not,
     * returning the address in String form.
     */
    public static ServerName getServerName(InetSocketAddress addr) {
        String host = addr.getHostString();
        byte[] literal = IPAddressUtil.textToNumericFormatV4(host);
        if (literal == null) {
            // not IPv4 literal. Check IPv6
            literal = IPAddressUtil.textToNumericFormatV6(host);
            return new ServerName(host, literal != null);
        } else {
            return new ServerName(host, true);
        }
    }

    private static boolean isLoopbackLiteral(byte[] bytes) {
        if (bytes.length == 4) {
            return bytes[0] == 127;
        } else if (bytes.length == 16) {
            for (int i=0; i<14; i++)
                if (bytes[i] != 0)
                    return false;
            if (bytes[15] != 1)
                return false;
            return true;
        } else
            throw new InternalError();
    }

    /*
     * Validates a RFC 7230 field-value.
     *
     * "Obsolete line folding" rule
     *
     *     obs-fold = CRLF 1*( SP / HTAB )
     *
     * is not permitted!
     */
    public static boolean isValidValue(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c > 255) {
                return false;
            }
            if (c == ' ' || c == '\t') {
                continue;
            } else if (!fieldvchar[c]) {
                return false; // forbidden byte
            }
        }
        return true;
    }


    @SuppressWarnings("removal")
    public static int getIntegerNetProperty(String name, int defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<Integer>) () ->
                NetProperties.getInteger(name, defaultValue));
    }

    @SuppressWarnings("removal")
    public static String getNetProperty(String name) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () ->
                NetProperties.get(name));
    }

    @SuppressWarnings("removal")
    public static boolean getBooleanProperty(String name, boolean def) {
        return AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
                Boolean.parseBoolean(System.getProperty(name, String.valueOf(def))));
    }

    @SuppressWarnings("removal")
    public static String getProperty(String name) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () ->
                System.getProperty(name));
    }

    @SuppressWarnings("removal")
    public static int getIntegerProperty(String name, int defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<Integer>) () ->
                Integer.parseInt(System.getProperty(name, String.valueOf(defaultValue))));
    }

    public static SSLParameters copySSLParameters(SSLParameters p) {
        SSLParameters p1 = new SSLParameters();
        p1.setAlgorithmConstraints(p.getAlgorithmConstraints());
        p1.setCipherSuites(p.getCipherSuites());
        // JDK 8 EXCL START
        p1.setEnableRetransmissions(p.getEnableRetransmissions());
        p1.setMaximumPacketSize(p.getMaximumPacketSize());
        // JDK 8 EXCL END
        p1.setEndpointIdentificationAlgorithm(p.getEndpointIdentificationAlgorithm());
        p1.setNeedClientAuth(p.getNeedClientAuth());
        String[] protocols = p.getProtocols();
        if (protocols != null) {
            p1.setProtocols(protocols.clone());
        }
        p1.setSNIMatchers(p.getSNIMatchers());
        p1.setServerNames(p.getServerNames());
        p1.setUseCipherSuitesOrder(p.getUseCipherSuitesOrder());
        p1.setWantClientAuth(p.getWantClientAuth());
        return p1;
    }

    /**
     * Set limit to position, and position to mark.
     */
    public static void flipToMark(ByteBuffer buffer, int mark) {
        buffer.limit(buffer.position());
        buffer.position(mark);
    }

    public static String stackTrace(Throwable t) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(bos, true, US_ASCII);
        t.printStackTrace(p);
        return bos.toString(US_ASCII);
    }

    /**
     * Copies as much of src to dst as possible.
     * Return number of bytes copied
     */
    public static int copy(ByteBuffer src, ByteBuffer dst) {
        int srcLen = src.remaining();
        int dstLen = dst.remaining();
        if (srcLen > dstLen) {
            int diff = srcLen - dstLen;
            int limit = src.limit();
            src.limit(limit - diff);
            dst.put(src);
            src.limit(limit);
        } else {
            dst.put(src);
        }
        return srcLen - src.remaining();
    }

    /** Threshold beyond which data is no longer copied into the current
     * buffer, if that buffer has enough unused space. */
    private static final int COPY_THRESHOLD = 8192;

    /**
     * Adds the data from buffersToAdd to currentList. Either 1) appends the
     * data from a particular buffer to the last buffer in the list ( if
     * there is enough unused space ), or 2) adds it to the list.
     *
     * @return the number of bytes added
     */
    public static long accumulateBuffers(List<ByteBuffer> currentList,
                                         List<ByteBuffer> buffersToAdd) {
        long accumulatedBytes = 0;
        for (ByteBuffer bufferToAdd : buffersToAdd) {
            int remaining = bufferToAdd.remaining();
            if (remaining <= 0)
                continue;
            int listSize = currentList.size();
            if (listSize == 0) {
                currentList.add(bufferToAdd);
                accumulatedBytes = remaining;
                continue;
            }

            ByteBuffer lastBuffer = currentList.get(listSize - 1);
            int freeSpace = lastBuffer.capacity() - lastBuffer.limit();
            if (remaining <= COPY_THRESHOLD && freeSpace >= remaining) {
                // append the new data to the unused space in the last buffer
                int position = lastBuffer.position();
                int limit = lastBuffer.limit();
                lastBuffer.position(limit);
                lastBuffer.limit(limit + remaining);
                lastBuffer.put(bufferToAdd);
                lastBuffer.position(position);
            } else {
                currentList.add(bufferToAdd);
            }
            accumulatedBytes += remaining;
        }
        return accumulatedBytes;
    }

    public static ByteBuffer copy(ByteBuffer src) {
        ByteBuffer dst = ByteBuffer.allocate(src.remaining());
        dst.put(src);
        dst.flip();
        return dst;
    }

    public static ByteBuffer copyAligned(ByteBuffer src) {
        int len = src.remaining();
        int size = ((len + 7) >> 3) << 3;
        assert size >= len;
        ByteBuffer dst = ByteBuffer.allocate(size);
        dst.put(src);
        dst.flip();
        return dst;
    }

    public static String dump(Object... objects) {
        return Arrays.toString(objects);
    }

    public static String stringOf(Collection<?> source) {
        // We don't know anything about toString implementation of this
        // collection, so let's create an array
        return Arrays.toString(source.toArray());
    }

    public static long remaining(ByteBuffer[] bufs) {
        long remain = 0;
        for (ByteBuffer buf : bufs) {
            remain += buf.remaining();
        }
        return remain;
    }

    public static boolean hasRemaining(List<ByteBuffer> bufs) {
        for (ByteBuffer buf : bufs) {
            if (buf.hasRemaining())
                return true;
        }
        return false;
    }

    public static long remaining(List<ByteBuffer> bufs) {
        long remain = 0;
        for (ByteBuffer buf : bufs) {
            remain += buf.remaining();
        }
        return remain;
    }

    public static long synchronizedRemaining(List<ByteBuffer> bufs) {
        synchronized (bufs) {
            return remaining(bufs);
        }
    }

    public static int remaining(List<ByteBuffer> bufs, int max) {
        long remain = 0;
        for (ByteBuffer buf : bufs) {
            remain += buf.remaining();
            if (remain > max) {
                throw new IllegalArgumentException("too many bytes");
            }
        }
        return (int) remain;
    }

    public static int remaining(ByteBuffer[] refs, int max) {
        long remain = 0;
        for (ByteBuffer b : refs) {
            remain += b.remaining();
            if (remain > max) {
                throw new IllegalArgumentException("too many bytes");
            }
        }
        return (int) remain;
    }

    public static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (IOException ignored) { }
        }
    }

    // Put all these static 'empty' singletons here
    public static final ByteBuffer EMPTY_BYTEBUFFER = ByteBuffer.allocate(0);
    public static final ByteBuffer[] EMPTY_BB_ARRAY = new ByteBuffer[0];
    public static final List<ByteBuffer> EMPTY_BB_LIST = List.of();

    /**
     * Returns a slice of size {@code amount} from the given buffer. If the
     * buffer contains more data than {@code amount}, then the slice's capacity
     * ( and, but not just, its limit ) is set to {@code amount}. If the buffer
     * does not contain more data than {@code amount}, then the slice's capacity
     * will be the same as the given buffer's capacity.
     */
    public static ByteBuffer sliceWithLimitedCapacity(ByteBuffer buffer, int amount) {
        final int index = buffer.position() + amount;
        final int limit = buffer.limit();
        if (index != limit) {
            // additional data in the buffer
            buffer.limit(index);  // ensures that the slice does not go beyond
        } else {
            // no additional data in the buffer
            buffer.limit(buffer.capacity());  // allows the slice full capacity
        }

        ByteBuffer newb = buffer.slice();
        buffer.position(index);
        buffer.limit(limit);    // restore the original buffer's limit
        newb.limit(amount);     // slices limit to amount (capacity may be greater)
        return newb;
    }

    /**
     * Get the Charset from the Content-encoding header. Defaults to
     * UTF_8
     */
    public static Charset charsetFrom(HttpHeaders headers) {
        String type = headers.firstValue("Content-type")
                .orElse("text/html; charset=utf-8");
        int i = type.indexOf(";");
        if (i >= 0) type = type.substring(i+1);
        try {
            HeaderParser parser = new HeaderParser(type);
            String value = parser.findValue("charset");
            if (value == null) return StandardCharsets.UTF_8;
            return Charset.forName(value);
        } catch (Throwable x) {
            Log.logTrace("Can't find charset in \"{0}\" ({1})", type, x);
            return StandardCharsets.UTF_8;
        }
    }

    public static UncheckedIOException unchecked(IOException e) {
        return new UncheckedIOException(e);
    }

    /**
     * Get a logger for debug HTTP traces.
     *
     * The logger should only be used with levels whose severity is
     * {@code <= DEBUG}. By default, this logger will forward all messages
     * logged to an internal logger named "jdk.internal.httpclient.debug".
     * In addition, if the property -Djdk.internal.httpclient.debug=true is set,
     * it will print the messages on stderr.
     * The logger will add some decoration to the printed message, in the form of
     * {@code <Level>:[<thread-name>] [<elapsed-time>] <dbgTag>: <formatted message>}
     *
     * @param dbgTag A lambda that returns a string that identifies the caller
     *               (e.g: "SocketTube(3)", or "Http2Connection(SocketTube(3))")
     *
     * @return A logger for HTTP internal debug traces
     */
    public static Logger getDebugLogger(Supplier<String> dbgTag) {
        return getDebugLogger(dbgTag, DEBUG);
    }

    /**
     * Get a logger for debug HTTP traces.The logger should only be used
     * with levels whose severity is {@code <= DEBUG}.
     *
     * By default, this logger will forward all messages logged to an internal
     * logger named "jdk.internal.httpclient.debug".
     * In addition, if the message severity level is >= to
     * the provided {@code errLevel} it will print the messages on stderr.
     * The logger will add some decoration to the printed message, in the form of
     * {@code <Level>:[<thread-name>] [<elapsed-time>] <dbgTag>: <formatted message>}
     *
     * @apiNote To obtain a logger that will always print things on stderr in
     *          addition to forwarding to the internal logger, use
     *          {@code getDebugLogger(this::dbgTag, Level.ALL);}.
     *          This is also equivalent to calling
     *          {@code getDebugLogger(this::dbgTag, true);}.
     *          To obtain a logger that will only forward to the internal logger,
     *          use {@code getDebugLogger(this::dbgTag, Level.OFF);}.
     *          This is also equivalent to calling
     *          {@code getDebugLogger(this::dbgTag, false);}.
     *
     * @param dbgTag A lambda that returns a string that identifies the caller
     *               (e.g: "SocketTube(3)", or "Http2Connection(SocketTube(3))")
     * @param errLevel The level above which messages will be also printed on
     *               stderr (in addition to be forwarded to the internal logger).
     *
     * @return A logger for HTTP internal debug traces
     */
    static Logger getDebugLogger(Supplier<String> dbgTag, Level errLevel) {
        return DebugLogger.createHttpLogger(dbgTag, Level.OFF, errLevel);
    }

    /**
     * Get a logger for debug HTTP traces.The logger should only be used
     * with levels whose severity is {@code <= DEBUG}.
     *
     * By default, this logger will forward all messages logged to an internal
     * logger named "jdk.internal.httpclient.debug".
     * In addition, the provided boolean {@code on==true}, it will print the
     * messages on stderr.
     * The logger will add some decoration to the printed message, in the form of
     * {@code <Level>:[<thread-name>] [<elapsed-time>] <dbgTag>: <formatted message>}
     *
     * @apiNote To obtain a logger that will always print things on stderr in
     *          addition to forwarding to the internal logger, use
     *          {@code getDebugLogger(this::dbgTag, true);}.
     *          This is also equivalent to calling
     *          {@code getDebugLogger(this::dbgTag, Level.ALL);}.
     *          To obtain a logger that will only forward to the internal logger,
     *          use {@code getDebugLogger(this::dbgTag, false);}.
     *          This is also equivalent to calling
     *          {@code getDebugLogger(this::dbgTag, Level.OFF);}.
     *
     * @param dbgTag A lambda that returns a string that identifies the caller
     *               (e.g: "SocketTube(3)", or "Http2Connection(SocketTube(3))")
     * @param on  Whether messages should also be printed on
     *               stderr (in addition to be forwarded to the internal logger).
     *
     * @return A logger for HTTP internal debug traces
     */
    public static Logger getDebugLogger(Supplier<String> dbgTag, boolean on) {
        Level errLevel = on ? Level.ALL : Level.OFF;
        return getDebugLogger(dbgTag, errLevel);
    }

    /**
     * Return the host string from a HttpRequestImpl
     *
     * @param request
     * @return
     */
    public static String hostString(HttpRequestImpl request) {
        URI uri = request.uri();
        int port = uri.getPort();
        String host = uri.getHost();

        boolean defaultPort;
        if (port == -1) {
            defaultPort = true;
        } else if (uri.getScheme().equalsIgnoreCase("https")) {
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

    /**
     * Get a logger for debug HPACK traces.The logger should only be used
     * with levels whose severity is {@code <= DEBUG}.
     *
     * By default, this logger will forward all messages logged to an internal
     * logger named "jdk.internal.httpclient.hpack.debug".
     * In addition, if the message severity level is >= to
     * the provided {@code errLevel} it will print the messages on stderr.
     * The logger will add some decoration to the printed message, in the form of
     * {@code <Level>:[<thread-name>] [<elapsed-time>] <dbgTag>: <formatted message>}
     *
     * @apiNote To obtain a logger that will always print things on stderr in
     *          addition to forwarding to the internal logger, use
     *          {@code getHpackLogger(this::dbgTag, Level.ALL);}.
     *          This is also equivalent to calling
     *          {@code getHpackLogger(this::dbgTag, true);}.
     *          To obtain a logger that will only forward to the internal logger,
     *          use {@code getHpackLogger(this::dbgTag, Level.OFF);}.
     *          This is also equivalent to calling
     *          {@code getHpackLogger(this::dbgTag, false);}.
     *
     * @param dbgTag A lambda that returns a string that identifies the caller
     *               (e.g: "Http2Connection(SocketTube(3))/hpack.Decoder(3)")
     * @param errLevel The level above which messages will be also printed on
     *               stderr (in addition to be forwarded to the internal logger).
     *
     * @return A logger for HPACK internal debug traces
     */
    public static Logger getHpackLogger(Supplier<String> dbgTag, Level errLevel) {
        Level outLevel = Level.OFF;
        return DebugLogger.createHpackLogger(dbgTag, outLevel, errLevel);
    }

    /**
     * Get a logger for debug HPACK traces.The logger should only be used
     * with levels whose severity is {@code <= DEBUG}.
     *
     * By default, this logger will forward all messages logged to an internal
     * logger named "jdk.internal.httpclient.hpack.debug".
     * In addition, the provided boolean {@code on==true}, it will print the
     * messages on stderr.
     * The logger will add some decoration to the printed message, in the form of
     * {@code <Level>:[<thread-name>] [<elapsed-time>] <dbgTag>: <formatted message>}
     *
     * @apiNote To obtain a logger that will always print things on stderr in
     *          addition to forwarding to the internal logger, use
     *          {@code getHpackLogger(this::dbgTag, true);}.
     *          This is also equivalent to calling
     *          {@code getHpackLogger(this::dbgTag, Level.ALL);}.
     *          To obtain a logger that will only forward to the internal logger,
     *          use {@code getHpackLogger(this::dbgTag, false);}.
     *          This is also equivalent to calling
     *          {@code getHpackLogger(this::dbgTag, Level.OFF);}.
     *
     * @param dbgTag A lambda that returns a string that identifies the caller
     *               (e.g: "Http2Connection(SocketTube(3))/hpack.Decoder(3)")
     * @param on  Whether messages should also be printed on
     *            stderr (in addition to be forwarded to the internal logger).
     *
     * @return A logger for HPACK internal debug traces
     */
    public static Logger getHpackLogger(Supplier<String> dbgTag, boolean on) {
        Level errLevel = on ? Level.ALL : Level.OFF;
        return getHpackLogger(dbgTag, errLevel);
    }

    /**
     * Get a logger for debug WebSocket traces.The logger should only be used
     * with levels whose severity is {@code <= DEBUG}.
     *
     * By default, this logger will forward all messages logged to an internal
     * logger named "jdk.internal.httpclient.websocket.debug".
     * In addition, if the message severity level is >= to
     * the provided {@code errLevel} it will print the messages on stderr.
     * The logger will add some decoration to the printed message, in the form of
     * {@code <Level>:[<thread-name>] [<elapsed-time>] <dbgTag>: <formatted message>}
     *
     * @apiNote To obtain a logger that will always print things on stderr in
     *          addition to forwarding to the internal logger, use
     *          {@code getWebSocketLogger(this::dbgTag, Level.ALL);}.
     *          This is also equivalent to calling
     *          {@code getWSLogger(this::dbgTag, true);}.
     *          To obtain a logger that will only forward to the internal logger,
     *          use {@code getWebSocketLogger(this::dbgTag, Level.OFF);}.
     *          This is also equivalent to calling
     *          {@code getWSLogger(this::dbgTag, false);}.
     *
     * @param dbgTag A lambda that returns a string that identifies the caller
     *               (e.g: "WebSocket(3)")
     * @param errLevel The level above which messages will be also printed on
     *               stderr (in addition to be forwarded to the internal logger).
     *
     * @return A logger for HPACK internal debug traces
     */
    public static Logger getWebSocketLogger(Supplier<String> dbgTag, Level errLevel) {
        Level outLevel = Level.OFF;
        return DebugLogger.createWebSocketLogger(dbgTag, outLevel, errLevel);
    }

    /**
     * Get a logger for debug WebSocket traces.The logger should only be used
     * with levels whose severity is {@code <= DEBUG}.
     *
     * By default, this logger will forward all messages logged to an internal
     * logger named "jdk.internal.httpclient.websocket.debug".
     * In addition, the provided boolean {@code on==true}, it will print the
     * messages on stderr.
     * The logger will add some decoration to the printed message, in the form of
     * {@code <Level>:[<thread-name>] [<elapsed-time>] <dbgTag>: <formatted message>}
     *
     * @apiNote To obtain a logger that will always print things on stderr in
     *          addition to forwarding to the internal logger, use
     *          {@code getWebSocketLogger(this::dbgTag, true);}.
     *          This is also equivalent to calling
     *          {@code getWebSocketLogger(this::dbgTag, Level.ALL);}.
     *          To obtain a logger that will only forward to the internal logger,
     *          use {@code getWebSocketLogger(this::dbgTag, false);}.
     *          This is also equivalent to calling
     *          {@code getHpackLogger(this::dbgTag, Level.OFF);}.
     *
     * @param dbgTag A lambda that returns a string that identifies the caller
     *               (e.g: "WebSocket(3)")
     * @param on  Whether messages should also be printed on
     *            stderr (in addition to be forwarded to the internal logger).
     *
     * @return A logger for WebSocket internal debug traces
     */
    public static Logger getWebSocketLogger(Supplier<String> dbgTag, boolean on) {
        Level errLevel = on ? Level.ALL : Level.OFF;
        return getWebSocketLogger(dbgTag, errLevel);
    }

    /**
     * SSLSessions returned to user are wrapped in an immutable object
     */
    public static SSLSession immutableSession(SSLSession session) {
        if (session instanceof ExtendedSSLSession)
            return new ImmutableExtendedSSLSession((ExtendedSSLSession)session);
        else
            return new ImmutableSSLSession(session);
    }

    /**
     * Enabled by default. May be disabled for testing. Use with care
     */
    public static boolean isHostnameVerificationDisabled() {
        return isHostnameVerificationDisabled;
    }

    public static InetSocketAddress resolveAddress(InetSocketAddress address) {
        if (address != null && address.isUnresolved()) {
            // The default proxy selector may select a proxy whose  address is
            // unresolved. We must resolve the address before connecting to it.
            address = new InetSocketAddress(address.getHostString(), address.getPort());
        }
        return address;
    }

    public static Throwable toConnectException(Throwable e) {
        if (e == null) return null;
        e = getCompletionCause(e);
        if (e instanceof ConnectException) return e;
        if (e instanceof SecurityException) return e;
        if (e instanceof SSLException) return e;
        if (e instanceof Error) return e;
        if (e instanceof HttpTimeoutException) return e;
        Throwable cause = e;
        e = new ConnectException(e.getMessage());
        e.initCause(cause);
        return e;
    }

    /**
     * Returns the smallest (closest to zero) positive number {@code m} (which
     * is also a power of 2) such that {@code n <= m}.
     * <pre>{@code
     *          n  pow2Size(n)
     * -----------------------
     *          0           1
     *          1           1
     *          2           2
     *          3           4
     *          4           4
     *          5           8
     *          6           8
     *          7           8
     *          8           8
     *          9          16
     *         10          16
     *        ...         ...
     * 2147483647  1073741824
     * } </pre>
     *
     * The result is capped at {@code 1 << 30} as beyond that int wraps.
     *
     * @param n
     *         capacity
     *
     * @return the size of the array
     * @apiNote Used to size arrays in circular buffers (rings), usually in
     * order to squeeze extra performance substituting {@code %} operation for
     * {@code &}, which is up to 2 times faster.
     */
    public static int pow2Size(int n) {
        if (n < 0) {
            throw new IllegalArgumentException();
        } else if (n == 0) {
            return 1;
        } else if (n >= (1 << 30)) { // 2^31 is a negative int
            return 1 << 30;
        } else {
            return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
        }
    }

    // -- toAsciiString-like support to encode path and query URI segments

    // Encodes all characters >= \u0080 into escaped, normalized UTF-8 octets,
    // assuming that s is otherwise legal
    //
    public static String encode(String s) {
        int n = s.length();
        if (n == 0)
            return s;

        // First check whether we actually need to encode
        for (int i = 0;;) {
            if (s.charAt(i) >= '\u0080')
                break;
            if (++i >= n)
                return s;
        }

        String ns = Normalizer.normalize(s, Normalizer.Form.NFC);
        ByteBuffer bb = null;
        try {
            bb = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(ns));
        } catch (CharacterCodingException x) {
            assert false : x;
        }

        HexFormat format = HexFormat.of().withUpperCase();
        StringBuilder sb = new StringBuilder();
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80) {
                sb.append('%');
                format.toHexDigits(sb, (byte)b);
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }
}
