/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.net.http;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import jdk.internal.net.http.HttpRequestBuilderImpl;
import jdk.internal.net.http.RequestPublishers;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An HTTP request.
 *
 * <p> An {@code HttpRequest} instance is built through an {@code HttpRequest}
 * {@linkplain HttpRequest.Builder builder}. An {@code HttpRequest} builder
 * is obtained from one of the {@link HttpRequest#newBuilder(URI) newBuilder}
 * methods. A request's {@link URI}, headers, and body can be set. Request
 * bodies are provided through a {@link BodyPublisher BodyPublisher} supplied
 * to one of the {@link Builder#POST(BodyPublisher) POST},
 * {@link Builder#PUT(BodyPublisher) PUT} or
 * {@link Builder#method(String,BodyPublisher) method} methods.
 * Once all required parameters have been set in the builder, {@link
 * Builder#build() build} will return the {@code HttpRequest}. Builders can be
 * copied and modified many times in order to build multiple related requests
 * that differ in some parameters.
 *
 * <p> The following is an example of a GET request that prints the response
 * body as a String:
 *
 * <pre>{@code    HttpClient client = HttpClient.newHttpClient();
 *   HttpRequest request = HttpRequest.newBuilder()
 *         .uri(URI.create("http://foo.com/"))
 *         .build();
 *   client.sendAsync(request, BodyHandlers.ofString())
 *         .thenApply(HttpResponse::body)
 *         .thenAccept(System.out::println)
 *         .join(); }</pre>
 *
 * <p>The class {@link BodyPublishers BodyPublishers} provides implementations
 * of many common publishers. Alternatively, a custom {@code BodyPublisher}
 * implementation can be used.
 *
 * @since 11
 */
public abstract class HttpRequest {

    /**
     * Creates an HttpRequest.
     */
    protected HttpRequest() {}

    /**
     * A builder of {@linkplain HttpRequest HTTP requests}.
     *
     * <p> Instances of {@code HttpRequest.Builder} are created by calling
     * {@link HttpRequest#newBuilder()}, {@link HttpRequest#newBuilder(URI)},
     * or {@link HttpRequest#newBuilder(HttpRequest, BiPredicate)}.
     *
     * <p> The builder can be used to configure per-request state, such as: the
     * request URI, the request method (default is GET unless explicitly set),
     * specific request headers, etc. Each of the setter methods modifies the
     * state of the builder and returns the same instance. The methods are not
     * synchronized and should not be called from multiple threads without
     * external synchronization. The {@link #build() build} method returns a new
     * {@code HttpRequest} each time it is invoked. Once built an {@code
     * HttpRequest} is immutable, and can be sent multiple times.
     *
     * <p> Note, that not all request headers may be set by user code. Some are
     * restricted for security reasons and others such as the headers relating
     * to authentication, redirection and cookie management may be managed by
     * specific APIs rather than through directly user set headers.
     *
     * @since 11
     */
    public interface Builder {

        /**
         * Sets this {@code HttpRequest}'s request {@code URI}.
         *
         * @param uri the request URI
         * @return this builder
         * @throws IllegalArgumentException if the {@code URI} scheme is not
         *         supported
         */
        public Builder uri(URI uri);

        /**
         * Requests the server to acknowledge the request before sending the
         * body. This is disabled by default. If enabled, the server is
         * requested to send an error response or a {@code 100 Continue}
         * response before the client sends the request body. This means the
         * request publisher for the request will not be invoked until this
         * interim response is received.
         *
         * @param enable {@code true} if Expect continue to be sent
         * @return this builder
         */
        public Builder expectContinue(boolean enable);

        /**
         * Sets the preferred {@link HttpClient.Version} for this request.
         *
         * <p> The corresponding {@link HttpResponse} should be checked for the
         * version that was actually used. If the version is not set in a
         * request, then the version requested will be that of the sending
         * {@link HttpClient}.
         *
         * @param version the HTTP protocol version requested
         * @return this builder
         */
        public Builder version(HttpClient.Version version);

        /**
         * Adds the given name value pair to the set of headers for this request.
         * The given value is added to the list of values for that name.
         *
         * @implNote An implementation may choose to restrict some header names
         *           or values, as the HTTP Client may determine their value itself.
         *           For example, "Content-Length", which will be determined by
         *           the request Publisher. In such a case, an implementation of
         *           {@code HttpRequest.Builder} may choose to throw an
         *           {@code IllegalArgumentException} if such a header is passed
         *           to the builder.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder
         * @throws IllegalArgumentException if the header name or value is not
         *         valid, see <a href="https://tools.ietf.org/html/rfc7230#section-3.2">
         *         RFC 7230 section-3.2</a>, or the header name or value is restricted
         *         by the implementation.
         */
        public Builder header(String name, String value);

        /**
         * Adds the given name value pairs to the set of headers for this
         * request. The supplied {@code String} instances must alternate as
         * header names and header values.
         * To add several values to the same name then the same name must
         * be supplied with each new value.
         *
         * @param headers the list of name value pairs
         * @return this builder
         * @throws IllegalArgumentException if there are an odd number of
         *         parameters, or if a header name or value is not valid, see
         *         <a href="https://tools.ietf.org/html/rfc7230#section-3.2">
         *         RFC 7230 section-3.2</a>, or a header name or value is
         *         {@linkplain #header(String, String) restricted} by the
         *         implementation.
         */
        public Builder headers(String... headers);

        /**
         * Sets a timeout for this request. If the response is not received
         * within the specified timeout then an {@link HttpTimeoutException} is
         * thrown from {@link HttpClient#send(java.net.http.HttpRequest,
         * java.net.http.HttpResponse.BodyHandler) HttpClient::send} or
         * {@link HttpClient#sendAsync(java.net.http.HttpRequest,
         * java.net.http.HttpResponse.BodyHandler) HttpClient::sendAsync}
         * completes exceptionally with an {@code HttpTimeoutException}. The effect
         * of not setting a timeout is the same as setting an infinite Duration,
         * i.e. block forever.
         *
         * @param duration the timeout duration
         * @return this builder
         * @throws IllegalArgumentException if the duration is non-positive
         */
        public abstract Builder timeout(Duration duration);

        /**
         * Sets the given name value pair to the set of headers for this
         * request. This overwrites any previously set values for name.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder
         * @throws IllegalArgumentException if the header name or value is not valid,
         *         see <a href="https://tools.ietf.org/html/rfc7230#section-3.2">
         *         RFC 7230 section-3.2</a>, or the header name or value is
         *         {@linkplain #header(String, String) restricted} by the
         *         implementation.
         */
        public Builder setHeader(String name, String value);

        /**
         * Sets the request method of this builder to GET.
         * This is the default.
         *
         * @return this builder
         */
        public Builder GET();

        /**
         * Sets the request method of this builder to POST and sets its
         * request body publisher to the given value.
         *
         * @param bodyPublisher the body publisher
         *
         * @return this builder
         */
        public Builder POST(BodyPublisher bodyPublisher);

        /**
         * Sets the request method of this builder to PUT and sets its
         * request body publisher to the given value.
         *
         * @param bodyPublisher the body publisher
         *
         * @return this builder
         */
        public Builder PUT(BodyPublisher bodyPublisher);

        /**
         * Sets the request method of this builder to DELETE.
         *
         * @return this builder
         */
        public Builder DELETE();

        /**
         * Sets the request method and request body of this builder to the
         * given values.
         *
         * @apiNote The {@link BodyPublishers#noBody() noBody} request
         * body publisher can be used where no request body is required or
         * appropriate. Whether a method is restricted, or not, is
         * implementation specific. For example, some implementations may choose
         * to restrict the {@code CONNECT} method.
         *
         * @param method the method to use
         * @param bodyPublisher the body publisher
         * @return this builder
         * @throws IllegalArgumentException if the method name is not
         *         valid, see <a href="https://tools.ietf.org/html/rfc7230#section-3.1.1">
         *         RFC 7230 section-3.1.1</a>, or the method is restricted by the
         *         implementation.
         */
        public Builder method(String method, BodyPublisher bodyPublisher);

        /**
         * Builds and returns an {@link HttpRequest}.
         *
         * @return a new {@code HttpRequest}
         * @throws IllegalStateException if a URI has not been set
         */
        public HttpRequest build();

        /**
         * Returns an exact duplicate copy of this {@code Builder} based on
         * current state. The new builder can then be modified independently of
         * this builder.
         *
         * @return an exact copy of this builder
         */
        public Builder copy();
    }

    /**
     * Creates an {@code HttpRequest} builder with the given URI.
     *
     * @param uri the request URI
     * @return a new request builder
     * @throws IllegalArgumentException if the URI scheme is not supported.
     */
    public static HttpRequest.Builder newBuilder(URI uri) {
        return new HttpRequestBuilderImpl(uri);
    }

    /**
     * Creates a {@code Builder} whose initial state is copied from an existing
     * {@code HttpRequest}.
     *
     * <p> This builder can be used to build an {@code HttpRequest}, equivalent
     * to the original, while allowing amendment of the request state prior to
     * construction - for example, adding additional headers.
     *
     * <p> The {@code filter} is applied to each header name value pair as they
     * are copied from the given request. When completed, only headers that
     * satisfy the condition as laid out by the {@code filter} will be present
     * in the {@code Builder} returned from this method.
     *
     * @apiNote
     * The following scenarios demonstrate typical use-cases of the filter.
     * Given an {@code HttpRequest} <em>request</em>:
     * <br><br>
     * <ul>
     *  <li> Retain all headers:
     *  <pre>{@code HttpRequest.newBuilder(request, (n, v) -> true)}</pre>
     *
     *  <li> Remove all headers:
     *  <pre>{@code HttpRequest.newBuilder(request, (n, v) -> false)}</pre>
     *
     *  <li> Remove a particular header (e.g. Foo-Bar):
     *  <pre>{@code HttpRequest.newBuilder(request, (name, value) -> !name.equalsIgnoreCase("Foo-Bar"))}</pre>
     * </ul>
     *
     * @param request the original request
     * @param filter a header filter
     * @return a new request builder
     * @throws IllegalArgumentException if a new builder cannot be seeded from
     *         the given request (for instance, if the request contains illegal
     *         parameters)
     * @since 16
     */
    public static Builder newBuilder(HttpRequest request, BiPredicate<String, String> filter) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(filter);

        final HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(request.uri());
        builder.expectContinue(request.expectContinue());

        // Filter unwanted headers
        HttpHeaders headers = HttpHeaders.of(request.headers().map(), filter);
        headers.map().forEach((name, values) ->
                values.forEach(value -> builder.header(name, value)));

        request.version().ifPresent(builder::version);
        request.timeout().ifPresent(builder::timeout);
        var method = request.method();
        request.bodyPublisher().ifPresentOrElse(
                // if body is present, set it
                bodyPublisher -> builder.method(method, bodyPublisher),
                // otherwise, the body is absent, special case for GET/DELETE,
                // or else use empty body
                () -> {
                    switch (method) {
                        case "GET" -> builder.GET();
                        case "DELETE" -> builder.DELETE();
                        default -> builder.method(method, HttpRequest.BodyPublishers.noBody());
                    }
                }
        );
        return builder;
    }

    /**
     * Creates an {@code HttpRequest} builder.
     *
     * @return a new request builder
     */
    public static HttpRequest.Builder newBuilder() {
        return new HttpRequestBuilderImpl();
    }

    /**
     * Returns an {@code Optional} containing the {@link BodyPublisher} set on
     * this request. If no {@code BodyPublisher} was set in the requests's
     * builder, then the {@code Optional} is empty.
     *
     * @return an {@code Optional} containing this request's {@code BodyPublisher}
     */
    public abstract Optional<BodyPublisher> bodyPublisher();

    /**
     * Returns the request method for this request. If not set explicitly,
     * the default method for any request is "GET".
     *
     * @return this request's method
     */
    public abstract String method();

    /**
     * Returns an {@code Optional} containing this request's timeout duration.
     * If the timeout duration was not set in the request's builder, then the
     * {@code Optional} is empty.
     *
     * @return an {@code Optional} containing this request's timeout duration
     */
    public abstract Optional<Duration> timeout();

    /**
     * Returns this request's {@linkplain HttpRequest.Builder#expectContinue(boolean)
     * expect continue} setting.
     *
     * @return this request's expect continue setting
     */
    public abstract boolean expectContinue();

    /**
     * Returns this request's {@code URI}.
     *
     * @return this request's URI
     */
    public abstract URI uri();

    /**
     * Returns an {@code Optional} containing the HTTP protocol version that
     * will be requested for this {@code HttpRequest}. If the version was not
     * set in the request's builder, then the {@code Optional} is empty.
     * In that case, the version requested will be that of the sending
     * {@link HttpClient}. The corresponding {@link HttpResponse} should be
     * queried to determine the version that was actually used.
     *
     * @return HTTP protocol version
     */
    public abstract Optional<HttpClient.Version> version();

    /**
     * The (user-accessible) request headers that this request was (or will be)
     * sent with.
     *
     * @return this request's HttpHeaders
     */
    public abstract HttpHeaders headers();

    /**
     * Tests this HTTP request instance for equality with the given object.
     *
     * <p> If the given object is not an {@code HttpRequest} then this
     * method returns {@code false}. Two HTTP requests are equal if their URI,
     * method, and headers fields are all equal.
     *
     * <p> This method satisfies the general contract of the {@link
     * Object#equals(Object) Object.equals} method.
     *
     * @param obj the object to which this object is to be compared
     * @return {@code true} if, and only if, the given object is an {@code
     *         HttpRequest} that is equal to this HTTP request
     */
    @Override
    public final boolean equals(Object obj) {
       if (! (obj instanceof HttpRequest))
           return false;
       HttpRequest that = (HttpRequest)obj;
       if (!that.method().equals(this.method()))
           return false;
       if (!that.uri().equals(this.uri()))
           return false;
       if (!that.headers().equals(this.headers()))
           return false;
       return true;
    }

    /**
     * Computes a hash code for this HTTP request instance.
     *
     * <p> The hash code is based upon the HTTP request's URI, method, and
     * header components, and satisfies the general contract of the
     * {@link Object#hashCode Object.hashCode} method.
     *
     * @return the hash-code value for this HTTP request
     */
    public final int hashCode() {
        return method().hashCode()
                + uri().hashCode()
                + headers().hashCode();
    }

    /**
     * A {@code BodyPublisher} converts high-level Java objects into a flow of
     * byte buffers suitable for sending as a request body.  The class
     * {@link BodyPublishers BodyPublishers} provides implementations of many
     * common publishers.
     *
     * <p> The {@code BodyPublisher} interface extends {@link Flow.Publisher
     * Flow.Publisher&lt;ByteBuffer&gt;}, which means that a {@code BodyPublisher}
     * acts as a publisher of {@linkplain ByteBuffer byte buffers}.
     *
     * <p> When sending a request that contains a body, the HTTP Client
     * subscribes to the request's {@code BodyPublisher} in order to receive the
     * flow of outgoing request body data. The normal semantics of {@link
     * Flow.Subscriber} and {@link Flow.Publisher} are implemented by the HTTP
     * Client and are expected from {@code BodyPublisher} implementations. Each
     * outgoing request results in one HTTP Client {@code Subscriber}
     * subscribing to the {@code BodyPublisher} in order to provide the sequence
     * of byte buffers containing the request body. Instances of {@code
     * ByteBuffer} published by the publisher must be allocated by the
     * publisher, and must not be accessed after being published to the HTTP
     * Client. These subscriptions complete normally when the request body is
     * fully sent, and can be canceled or terminated early through error. If a
     * request needs to be resent for any reason, then a new subscription is
     * created which is expected to generate the same data as before.
     *
     * <p> A {@code BodyPublisher} that reports a {@linkplain #contentLength()
     * content length} of {@code 0} may not be subscribed to by the HTTP Client,
     * as it has effectively no data to publish.
     *
     * @see BodyPublishers
     * @since 11
     */
    public interface BodyPublisher extends Flow.Publisher<ByteBuffer> {

        /**
         * Returns the content length for this request body. May be zero
         * if no request body being sent, greater than zero for a fixed
         * length content, or less than zero for an unknown content length.
         *
         * <p> This method may be invoked before the publisher is subscribed to.
         * This method may be invoked more than once by the HTTP client
         * implementation, and MUST return the same constant value each time.
         *
         * @return the content length for this request body, if known
         */
        long contentLength();
    }

    /**
     * Implementations of {@link BodyPublisher BodyPublisher} that implement
     * various useful publishers, such as publishing the request body from a
     * String, or from a file.
     *
     * <p> The following are examples of using the predefined body publishers to
     * convert common high-level Java objects into a flow of data suitable for
     * sending as a request body:
     *
     *  <pre>{@code    // Request body from a String
     *   HttpRequest request = HttpRequest.newBuilder()
     *        .uri(URI.create("https://foo.com/"))
     *        .header("Content-Type", "text/plain; charset=UTF-8")
     *        .POST(BodyPublishers.ofString("some body text"))
     *        .build();
     *
     *   // Request body from a File
     *   HttpRequest request = HttpRequest.newBuilder()
     *        .uri(URI.create("https://foo.com/"))
     *        .header("Content-Type", "application/json")
     *        .POST(BodyPublishers.ofFile(Paths.get("file.json")))
     *        .build();
     *
     *   // Request body from a byte array
     *   HttpRequest request = HttpRequest.newBuilder()
     *        .uri(URI.create("https://foo.com/"))
     *        .POST(BodyPublishers.ofByteArray(new byte[] { ... }))
     *        .build(); }</pre>
     *
     * @since 11
     */
    public static class BodyPublishers {

        private BodyPublishers() { }

        /**
         * Returns a request body publisher whose body is retrieved from the
         * given {@code Flow.Publisher}. The returned request body publisher
         * has an unknown content length.
         *
         * @apiNote This method can be used as an adapter between {@code
         * BodyPublisher} and {@code Flow.Publisher}, where the amount of
         * request body that the publisher will publish is unknown.
         *
         * @param publisher the publisher responsible for publishing the body
         * @return a BodyPublisher
         */
        public static BodyPublisher
        fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher) {
            return new RequestPublishers.PublisherAdapter(publisher, -1L);
        }

        /**
         * Returns a request body publisher whose body is retrieved from the
         * given {@code Flow.Publisher}. The returned request body publisher
         * has the given content length.
         *
         * <p> The given {@code contentLength} is a positive number, that
         * represents the exact amount of bytes the {@code publisher} must
         * publish.
         *
         * @apiNote This method can be used as an adapter between {@code
         * BodyPublisher} and {@code Flow.Publisher}, where the amount of
         * request body that the publisher will publish is known.
         *
         * @param publisher the publisher responsible for publishing the body
         * @param contentLength a positive number representing the exact
         *                      amount of bytes the publisher will publish
         * @throws IllegalArgumentException if the content length is
         *                                  non-positive
         * @return a BodyPublisher
         */
        public static BodyPublisher
        fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher,
                      long contentLength) {
            if (contentLength < 1)
                throw new IllegalArgumentException("non-positive contentLength: "
                        + contentLength);
            return new RequestPublishers.PublisherAdapter(publisher, contentLength);
        }

        /**
         * Returns a request body publisher whose body is the given {@code
         * String}, converted using the {@link StandardCharsets#UTF_8 UTF_8}
         * character set.
         *
         * @param body the String containing the body
         * @return a BodyPublisher
         */
        public static BodyPublisher ofString(String body) {
            return ofString(body, UTF_8);
        }

        /**
         * Returns a request body publisher whose body is the given {@code
         * String}, converted using the given character set.
         *
         * @param s the String containing the body
         * @param charset the character set to convert the string to bytes
         * @return a BodyPublisher
         */
        public static BodyPublisher ofString(String s, Charset charset) {
            return new RequestPublishers.StringPublisher(s, charset);
        }

        /**
         * A request body publisher that reads its data from an {@link
         * InputStream}. A {@link Supplier} of {@code InputStream} is used in
         * case the request needs to be repeated, as the content is not buffered.
         * The {@code Supplier} may return {@code null} on subsequent attempts,
         * in which case the request fails.
         *
         * @param streamSupplier a Supplier of open InputStreams
         * @return a BodyPublisher
         */
        // TODO (spec): specify that the stream will be closed
        public static BodyPublisher ofInputStream(Supplier<? extends InputStream> streamSupplier) {
            return new RequestPublishers.InputStreamPublisher(streamSupplier);
        }

        /**
         * Returns a request body publisher whose body is the given byte array.
         *
         * @param buf the byte array containing the body
         * @return a BodyPublisher
         */
        public static BodyPublisher ofByteArray(byte[] buf) {
            return new RequestPublishers.ByteArrayPublisher(buf);
        }

        /**
         * Returns a request body publisher whose body is the content of the
         * given byte array of {@code length} bytes starting from the specified
         * {@code offset}.
         *
         * @param buf the byte array containing the body
         * @param offset the offset of the first byte
         * @param length the number of bytes to use
         * @return a BodyPublisher
         * @throws IndexOutOfBoundsException if the sub-range is defined to be
         *                                   out of bounds
         */
        public static BodyPublisher ofByteArray(byte[] buf, int offset, int length) {
            Objects.checkFromIndexSize(offset, length, buf.length);
            return new RequestPublishers.ByteArrayPublisher(buf, offset, length);
        }

        /**
         * A request body publisher that takes data from the contents of a File.
         *
         * <p> Security manager permission checks are performed in this factory
         * method, when the {@code BodyPublisher} is created. Care must be taken
         * that the {@code BodyPublisher} is not shared with untrusted code.
         *
         * @param  path the path to the file containing the body
         * @return a BodyPublisher
         * @throws java.io.FileNotFoundException if the path is not found
         * @throws SecurityException if
         *         {@linkplain Files#newInputStream(Path, OpenOption...)
         *         opening the file for reading} is denied:
         *         in the case of the system-default file system provider,
         *         and a security manager is installed,
         *         {@link SecurityManager#checkRead(String) checkRead}
         *         is invoked to check read access to the given file
         */
        public static BodyPublisher ofFile(Path path) throws FileNotFoundException {
            Objects.requireNonNull(path);
            return RequestPublishers.FilePublisher.create(path);
        }

        /**
         * A request body publisher that takes data from an {@code Iterable}
         * of byte arrays. An {@link Iterable} is provided which supplies
         * {@link Iterator} instances. Each attempt to send the request results
         * in one invocation of the {@code Iterable}.
         *
         * @param iter an Iterable of byte arrays
         * @return a BodyPublisher
         */
        public static BodyPublisher ofByteArrays(Iterable<byte[]> iter) {
            return new RequestPublishers.IterablePublisher(iter);
        }

        /**
         * A request body publisher which sends no request body.
         *
         * @return a BodyPublisher which completes immediately and sends
         *         no request body.
         */
        public static BodyPublisher noBody() {
            return new RequestPublishers.EmptyPublisher();
        }

        /**
         * Returns a {@code BodyPublisher} that publishes a request
         * body consisting of the concatenation of the request bodies
         * published by a sequence of publishers.
         *
         * <p> If the sequence is empty an {@linkplain #noBody() empty} publisher
         * is returned. Otherwise, if the sequence contains a single element,
         * that publisher is returned. Otherwise a <em>concatenation publisher</em>
         * is returned.
         *
         * <p> The request body published by a <em>concatenation publisher</em>
         * is logically equivalent to the request body that would have
         * been published by concatenating all the bytes of each publisher
         * in sequence.
         *
         * <p> Each publisher is lazily subscribed to in turn,
         * until all the body bytes are published, an error occurs, or the
         * concatenation publisher's subscription is cancelled.
         * The concatenation publisher may be subscribed to more than once,
         * which in turn may result in the publishers in the sequence being
         * subscribed to more than once.
         *
         * <p> The concatenation publisher has a known content
         * length only if all publishers in the sequence have a known content
         * length. The {@link BodyPublisher#contentLength() contentLength}
         * reported by the concatenation publisher is computed as follows:
         * <ul>
         *     <li> If any of the publishers reports an <em>{@linkplain
         *         BodyPublisher#contentLength() unknown}</em> content length,
         *         or if the sum of the known content lengths would exceed
         *         {@link Long#MAX_VALUE}, the resulting
         *         content length is <em>unknown</em>.</li>
         *     <li> Otherwise, the resulting content length is the sum of the
         *         known content lengths, a number between
         *         {@code 0} and {@link Long#MAX_VALUE}, inclusive.</li>
         * </ul>
         *
         * @implNote If the concatenation publisher's subscription is
         * {@linkplain Flow.Subscription#cancel() cancelled}, or an error occurs
         * while publishing the bytes, not all publishers in the sequence may
         * be subscribed to.
         *
         * @param publishers a sequence of publishers.
         * @return An aggregate publisher that publishes a request body
         * logically equivalent to the concatenation of all bytes published
         * by each publisher in the sequence.
         *
         * @since 16
         */
        public static BodyPublisher concat(BodyPublisher... publishers) {
            return RequestPublishers.concat(Objects.requireNonNull(publishers));
        }
    }
}
