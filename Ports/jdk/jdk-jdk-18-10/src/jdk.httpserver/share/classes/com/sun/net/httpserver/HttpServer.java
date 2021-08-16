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

import com.sun.net.httpserver.spi.HttpServerProvider;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

/**
 * This class implements a simple HTTP server. A {@code HttpServer} is bound to an IP address
 * and port number and listens for incoming TCP connections from clients on this address.
 * The sub-class {@link HttpsServer} implements a server which handles HTTPS requests.
 *
 * <p>One or more {@link HttpHandler} objects must be associated with a server
 * in order to process requests. Each such {@code HttpHandler} is registered with
 * a root URI path which represents the location of the application or service
 * on this server. The mapping of a handler to a {@code HttpServer} is
 * encapsulated by a {@link HttpContext} object. HttpContexts are created by
 * calling {@link #createContext(String,HttpHandler)}.
 * Any request for which no handler can be found is rejected with a 404 response.
 * Management of threads can be done external to this object by providing a
 * {@link java.util.concurrent.Executor} object. If none is provided a default
 * implementation is used.
 *
 * <p> <a id="mapping_description"></a> <b>Mapping request URIs to HttpContext paths</b>
 *
 * <p>When a HTTP request is received, the appropriate {@code HttpContext}
 * (and handler) is located by finding the context whose path is the longest
 * matching prefix of the request URI's path. Paths are matched literally,
 * which means that the strings are compared case sensitively, and with no
 * conversion to or from any encoded forms. For example, given a {@code HttpServer}
 * with the following HttpContexts configured:
 *
 * <table class="striped"><caption style="display:none">description</caption>
 *      <thead>
 *          <tr>
 *              <th scope="col"><i>Context</i></th>
 *              <th scope="col"><i>Context path</i></th>
 *          </tr>
 *      </thead>
 *      <tbody>
 *              <tr><th scope="row">ctx1</th><td>"/"</td></tr>
 *              <tr><th scope="row">ctx2</th><td>"/apps/"</td></tr>
 *              <tr><th scope="row">ctx3</th><td>"/apps/foo/"</td></tr>
 *      </tbody>
 * </table>
 *
 * <p>The following table shows some request URIs and which, if any context they would
 * match with:
 * <table class="striped"><caption style="display:none">description</caption>
 *      <thead>
 *          <tr>
 *              <th scope="col"><i>Request URI</i></th>
 *              <th scope="col"><i>Matches context</i></th>
 *          </tr>
 *      </thead>
 *      <tbody>
 *          <tr><th scope="row">"http://foo.com/apps/foo/bar"</th><td>ctx3</td></tr>
 *          <tr><th scope="row">"http://foo.com/apps/Foo/bar"</th><td>no match, wrong case</td></tr>
 *          <tr><th scope="row">"http://foo.com/apps/app1"</th><td>ctx2</td></tr>
 *          <tr><th scope="row">"http://foo.com/foo"</th><td>ctx1</td></tr>
 *      </tbody>
 * </table>
 *
 * <p><b>Note about socket backlogs</b>
 *
 * <p>When binding to an address and port number, the application can also
 * specify an integer <i>backlog</i> parameter. This represents the maximum
 * number of incoming TCP connections which the system will queue internally.
 * Connections are queued while they are waiting to be accepted by the
 * {@code HttpServer}. When the limit is reached, further connections may be
 * rejected (or possibly ignored) by the underlying TCP implementation. Setting
 * the right backlog value is a compromise between efficient resource usage in
 * the TCP layer (not setting it too high) and allowing adequate throughput of
 * incoming requests (not setting it too low).
 *
 * @since 1.6
 */

public abstract class HttpServer {

    /**
     * Constructor for subclasses to call.
     */
    protected HttpServer() {
    }

    /**
     * Creates a {@code HttpServer} instance which is initially not bound to any
     * local address/port. The {@code HttpServer} is acquired from the currently
     * installed {@link HttpServerProvider}. The server must be bound using
     * {@link #bind(InetSocketAddress,int)} before it can be used.
     *
     * @throws IOException if an I/O error occurs
     * @return an instance of {@code HttpServer}
     */
    public static HttpServer create() throws IOException {
        return create (null, 0);
    }

    /**
     * Create a {@code HttpServer} instance which will bind to the
     * specified {@link java.net.InetSocketAddress} (IP address and port number).
     *
     * A maximum backlog can also be specified. This is the maximum number of
     * queued incoming connections to allow on the listening socket.
     * Queued TCP connections exceeding this limit may be rejected by the TCP
     * implementation. The {@code HttpServer} is acquired from the currently
     * installed {@link HttpServerProvider}
     *
     * @param addr the address to listen on, if {@code null} then
     *             {@link #bind(InetSocketAddress, int)} must be called to set
     *             the address
     * @param backlog the socket backlog. If this value is less than or equal to zero,
     *                then a system default value is used
     * @throws IOException if an I/O error occurs
     * @throws BindException if the server cannot bind to the requested address,
     * or if the server is already bound
     * @return an instance of {@code HttpServer}
     */

    public static HttpServer create(InetSocketAddress addr, int backlog) throws IOException {
        HttpServerProvider provider = HttpServerProvider.provider();
        return provider.createHttpServer (addr, backlog);
    }

    /**
     * Binds a currently unbound {@code HttpServer} to the given address and
     * port number. A maximum backlog can also be specified. This is the maximum
     * number of queued incoming connections to allow on the listening socket.
     * Queued TCP connections exceeding this limit may be rejected by the TCP
     * implementation.
     *
     * @param addr the address to listen on
     * @param backlog the socket backlog. If this value is less than or equal to
     *                zero, then a system default value is used
     * @throws BindException if the server cannot bind to the requested address
     * or if the server is already bound
     * @throws NullPointerException if addr is {@code null}
     */
    public abstract void bind(InetSocketAddress addr, int backlog) throws IOException;

    /**
     * Starts this server in a new background thread. The background thread
     * inherits the priority, thread group and context class loader
     * of the caller.
     */
    public abstract void start();

    /**
     * Sets this server's {@link java.util.concurrent.Executor} object. An
     * {@code Executor} must be established before {@link #start()} is called.
     * All HTTP requests are handled in tasks given to the executor.
     * If this method is not called (before {@link #start()}) or if it is called
     * with a {@code null Executor}, then a default implementation is used,
     * which uses the thread which was created by the {@link #start()} method.
     *
     * @param executor the {@code Executor} to set, or {@code null} for  default
     *                 implementation
     * @throws IllegalStateException if the server is already started
     */
    public abstract void setExecutor(Executor executor);


    /**
     * Returns this server's {@code Executor} object if one was specified with
     * {@link #setExecutor(Executor)}, or {@code null} if none was specified.
     *
     * @return the {@code Executor} established for this server or {@code null} if not set.
     */
    public abstract Executor getExecutor() ;

    /**
     * Stops this server by closing the listening socket and disallowing
     * any new exchanges from being processed. The method will then block
     * until all current exchange handlers have completed or else when
     * approximately <i>delay</i> seconds have elapsed (whichever happens
     * sooner). Then, all open TCP connections are closed, the background
     * thread created by {@link #start()} exits, and the method returns.
     * Once stopped, a {@code HttpServer} cannot be re-used.
     *
     * @param delay the maximum time in seconds to wait until exchanges have finished
     * @throws IllegalArgumentException if delay is less than zero
     */
    public abstract void stop(int delay);

    /**
     * Creates a {@code HttpContext}. A  {@code HttpContext} represents a mapping
     * from a URI path to a exchange handler on this  {@code HttpServer}. Once
     * created, all requests received by the server for the path will be handled
     * by calling the given handler object. The context is identified by the
     * path, and can later be removed from the server using this with the
     * {@link #removeContext(String)} method.
     *
     * <p> The path specifies the root URI path for this context. The first
     * character of path must be '/'.
     *
     * <p>The class overview describes how incoming request URIs are
     * <a href="#mapping_description">mapped</a> to HttpContext instances.
     *
     * @apiNote The path should generally, but is not required to, end with '/'.
     * If the path does not end with '/', eg such as with {@code "/foo"} then
     * this would match requests with a path of {@code "/foobar"} or
     * {@code "/foo/bar"}.
     *
     * @param path the root URI path to associate the context with
     * @param handler the handler to invoke for incoming requests
     * @throws IllegalArgumentException if path is invalid, or if a context
     * already exists for this path
     * @throws NullPointerException if either path, or handler are {@code null}
     * @return an instance of {@code HttpContext}
     */
    public abstract HttpContext createContext(String path, HttpHandler handler);

    /**
     * Creates a HttpContext without initially specifying a handler. The handler
     * must later be specified using {@link HttpContext#setHandler(HttpHandler)}.
     * A {@code HttpContext} represents a mapping from a URI path to an exchange
     * handler on this {@code HttpServer}. Once created, and when the handler has
     * been set, all requests received by the server for the path will be handled
     * by calling the handler object. The context is identified by the path, and
     * can later be removed from the server using this with the
     * {@link #removeContext(String)} method.
     *
     * <p>The path specifies the root URI path for this context. The first character of path must be
     * '/'.
     *
     * <p>The class overview describes how incoming request URIs are
     * <a href="#mapping_description">mapped</a> to {@code HttpContext} instances.
     *
     * @apiNote The path should generally, but is not required to, end with '/'.
     * If the path does not end with '/', eg such as with {@code "/foo"} then
     * this would match requests with a path of {@code "/foobar"} or
     * {@code "/foo/bar"}.
     *
     * @param path the root URI path to associate the context with
     * @throws IllegalArgumentException if path is invalid, or if a context
     * already exists for this path
     * @throws NullPointerException if path is {@code null}
     * @return an instance of {@code HttpContext}
     */
    public abstract HttpContext createContext(String path);

    /**
     * Removes the context identified by the given path from the server.
     * Removing a context does not affect exchanges currently being processed
     * but prevents new ones from being accepted.
     *
     * @param path the path of the handler to remove
     * @throws IllegalArgumentException if no handler corresponding to this
     * path exists.
     * @throws NullPointerException if path is {@code null}
     */
    public abstract void removeContext(String path) throws IllegalArgumentException;

    /**
     * Removes the given context from the server.
     * Removing a context does not affect exchanges currently being processed
     * but prevents new ones from being accepted.
     *
     * @param context the context to remove
     * @throws NullPointerException if context is {@code null}
     */
    public abstract void removeContext(HttpContext context);

    /**
     * Returns the address this server is listening on
     *
     * @return the {@code InetSocketAddress} the server is listening on
     */
    public abstract InetSocketAddress getAddress();
}
