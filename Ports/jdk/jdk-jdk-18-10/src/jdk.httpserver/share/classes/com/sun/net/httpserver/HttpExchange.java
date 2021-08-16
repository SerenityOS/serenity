/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.net.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

/**
 * This class encapsulates a HTTP request received and a
 * response to be generated in one exchange. It provides methods
 * for examining the request from the client, and for building and
 * sending the response.
 *
 * <p> The typical life-cycle of a {@code HttpExchange} is shown in the sequence
 * below:
 * <ol>
 *     <li>{@link #getRequestMethod()} to determine the command.
 *     <li>{@link #getRequestHeaders()} to examine the request headers (if
 *     needed).
 *     <li>{@link #getRequestBody()} returns an {@link InputStream} for
 *     reading the request body. After reading the request body, the stream
 *     should be closed.
 *     <li>{@link #getResponseHeaders()} to set any response headers, except
 *     content-length.
 *     <li>{@link #sendResponseHeaders(int,long)} to send the response headers.
 *     Must be called before next step.
 *     <li>{@link #getResponseBody()} to get a {@link OutputStream} to
 *     send the response body. When the response body has been written, the
 *     stream must be closed to terminate the exchange.
 * </ol>
 *
 * <b>Terminating exchanges</b>
 * <br>Exchanges are terminated when both the request {@code InputStream} and
 * response {@code OutputStream} are closed. Closing the {@code OutputStream},
 * implicitly closes the {@code InputStream} (if it is not already closed).
 * However, it is recommended to consume all the data from the {@code InputStream}
 * before closing it. The convenience method {@link #close()} does all of these
 * tasks. Closing an exchange without consuming all of the request body is not
 * an error but may make the underlying TCP connection unusable for following
 * exchanges. The effect of failing to terminate an exchange is undefined, but
 * will typically result in resources failing to be freed/reused.
 *
 * @since 1.6
 */

public abstract class HttpExchange implements AutoCloseable {

    /**
     * Constructor for subclasses to call.
     */
    protected HttpExchange() {
    }

    /**
     * Returns an immutable {@link Map} containing the HTTP headers that were
     * included with this request. The keys in this {@code Map} will be the header
     * names, while the values will be a {@link java.util.List} of
     * {@linkplain java.lang.String Strings} containing each value that was
     * included (either for a header that was listed several times, or one that
     * accepts a comma-delimited list of values on a single line). In either of
     * these cases, the values for the header name will be presented in the
     * order that they were included in the request.
     *
     * <p> The keys in {@code Map} are case-insensitive.
     *
     * @return a read-only {@code Map} which can be used to access request headers
     */
    public abstract Headers getRequestHeaders();

    /**
     * Returns a mutable {@link Map} into which the HTTP response headers can be
     * stored and which will be transmitted as part of this response. The keys in
     * the {@code Map} will be the header names, while the values must be a
     * {@link java.util.List} of {@linkplain java.lang.String Strings} containing
     * each value that should be included multiple times (in the order that they
     * should be included).
     *
     * <p> The keys in {@code Map} are case-insensitive.
     *
     * @return a writable {@code Map} which can be used to set response headers.
     */
    public abstract Headers getResponseHeaders();

    /**
     * Get the request {@link URI}.
     *
     * @return the request {@code URI}
     */
    public abstract URI getRequestURI();

    /**
     * Get the request method.
     *
     * @return the request method
     */
    public abstract String getRequestMethod();

    /**
     * Get the {@link HttpContext} for this exchange.
     *
     * @return the {@code HttpContext}
     */
    public abstract HttpContext getHttpContext();

    /**
     * Ends this exchange by doing the following in sequence:
     * <ol>
     *      <li> close the request {@link InputStream}, if not already closed.
     *      <li> close the response {@link OutputStream}, if not already closed.
     * </ol>
     */
    public abstract void close();

    /**
     * Returns a stream from which the request body can be read.
     * Multiple calls to this method will return the same stream.
     * It is recommended that applications should consume (read) all of the data
     * from this stream before closing it. If a stream is closed before all data
     * has been read, then the {@link InputStream#close()} call will read
     * and discard remaining data (up to an implementation specific number of
     * bytes).
     *
     * @return the stream from which the request body can be read
     */
    public abstract InputStream getRequestBody();

    /**
     * Returns a stream to which the response body must be
     * written. {@link #sendResponseHeaders(int,long)}) must be called prior to
     * calling this method. Multiple calls to this method (for the same exchange)
     * will return the same stream. In order to correctly terminate each exchange,
     * the output stream must be closed, even if no response body is being sent.
     *
     * <p> Closing this stream implicitly closes the {@link InputStream}
     * returned from {@link #getRequestBody()} (if it is not already closed).
     *
     * <p> If the call to {@link #sendResponseHeaders(int, long)} specified a
     * fixed response body length, then the exact number of bytes specified in
     * that call must be written to this stream. If too many bytes are written,
     * then the write method of {@link OutputStream} will throw an {@code IOException}.
     * If too few bytes are written then the stream
     * {@link OutputStream#close()} will throw an {@code IOException}.
     * In both cases, the exchange is aborted and the underlying TCP connection
     * closed.
     *
     * @return the stream to which the response body is written
     */
    public abstract OutputStream getResponseBody();


    /**
     * Starts sending the response back to the client using the current set of
     * response headers and the numeric response code as specified in this
     * method. The response body length is also specified as follows. If the
     * response length parameter is greater than {@code zero}, this specifies an
     * exact number of bytes to send and the application must send that exact
     * amount of data. If the response length parameter is {@code zero}, then
     * chunked transfer encoding is used and an arbitrary amount of data may be
     * sent. The application terminates the response body by closing the
     * {@link OutputStream}.
     * If response length has the value {@code -1} then no response body is
     * being sent.
     *
     * <p> If the content-length response header has not already been set then
     * this is set to the appropriate value depending on the response length
     * parameter.
     *
     * <p> This method must be called prior to calling {@link #getResponseBody()}.
     *
     * @implNote This implementation allows the caller to instruct the
     * server to force a connection close after the exchange terminates, by
     * supplying a {@code Connection: close} header to the {@linkplain
     * #getResponseHeaders() response headers} before {@code sendResponseHeaders}
     * is called.
     *
     * @param rCode          the response code to send
     * @param responseLength if {@literal > 0}, specifies a fixed response body
     *                       length and that exact number of bytes must be written
     *                       to the stream acquired from {@link #getResponseCode()}
     *                       If {@literal == 0}, then chunked encoding is used,
     *                       and an arbitrary number of bytes may be written.
     *                       If {@literal <= -1}, then no response body length is
     *                       specified and no response body may be written.
     * @throws IOException   if the response headers have already been sent or an I/O error occurs
     * @see   HttpExchange#getResponseBody()
     */
    public abstract void sendResponseHeaders(int rCode, long responseLength) throws IOException;

    /**
     * Returns the address of the remote entity invoking this request.
     *
     * @return the {@link InetSocketAddress} of the caller
     */
    public abstract InetSocketAddress getRemoteAddress();

    /**
     * Returns the response code, if it has already been set.
     *
     * @return the response code, if available. {@code -1} if not available yet.
     */
    public abstract int getResponseCode();

    /**
     * Returns the local address on which the request was received.
     *
     * @return the {@link InetSocketAddress} of the local interface
     */
    public abstract InetSocketAddress getLocalAddress();

    /**
     * Returns the protocol string from the request in the form
     * <i>protocol/majorVersion.minorVersion</i>. For example,
     * "{@code HTTP/1.1}".
     *
     * @return the protocol string from the request
     */
    public abstract String getProtocol();

    /**
     * {@link Filter} modules may store arbitrary objects with {@code HttpExchange}
     * instances as an out-of-band communication mechanism. Other filters
     * or the exchange handler may then access these objects.
     *
     * <p> Each {@code Filter} class will document the attributes which they make
     * available.
     *
     * @param name the name of the attribute to retrieve
     * @return the attribute object, or {@code null} if it does not exist
     * @throws NullPointerException if name is {@code null}
     */
    public abstract Object getAttribute(String name);

    /**
     * {@link Filter} modules may store arbitrary objects with {@code HttpExchange}
     * instances as an out-of-band communication mechanism. Other filters
     * or the exchange handler may then access these objects.
     *
     * <p> Each {@code Filter} class will document the attributes which they make
     * available.
     *
     * @param name  the name to associate with the attribute value
     * @param value the object to store as the attribute value. {@code null}
     *              value is permitted.
     * @throws NullPointerException if name is {@code null}
     */
    public abstract void setAttribute(String name, Object value);

    /**
     * Used by {@linkplain com.sun.net.httpserver.Filter Filters} to wrap either
     * (or both) of this exchange's {@link InputStream} and
     * {@link OutputStream}, with the given filtered streams so that
     * subsequent calls to {@link #getRequestBody()} will return the given
     * {@code InputStream}, and calls to {@link #getResponseBody()} will return
     * the given {@code OutputStream}. The streams provided to this call must wrap
     * the original streams, and may be (but are not required to be) sub-classes
     * of {@link java.io.FilterInputStream} and {@link java.io.FilterOutputStream}.
     *
     * @param i the filtered input stream to set as this object's
     *          {@code Inputstream}, or {@code null} if no change
     * @param o the filtered output stream to set as this object's
     *          {@code Outputstream}, or {@code null} if no change
     */
    public abstract void setStreams(InputStream i, OutputStream o);


    /**
     * If an authenticator is set on the {@link HttpContext} that owns this exchange,
     * then this method will return the {@link HttpPrincipal} that represents
     * the authenticated user for this {@code HttpExchange}.
     *
     * @return the {@code HttpPrincipal}, or {@code null} if no authenticator is set
     */
    public abstract HttpPrincipal getPrincipal();
}
