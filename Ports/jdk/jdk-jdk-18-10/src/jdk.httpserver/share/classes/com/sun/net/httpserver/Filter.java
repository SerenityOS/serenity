/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A filter used to pre- and post-process incoming requests. Pre-processing occurs
 * before the application's exchange handler is invoked, and post-processing
 * occurs after the exchange handler returns. Filters are organised in chains,
 * and are associated with {@link HttpContext} instances.
 *
 * <p> Each {@code Filter} in the chain, invokes the next filter within its own
 * {@link #doFilter(HttpExchange, Chain)} implementation. The final {@code Filter}
 * in the chain invokes the applications exchange handler.
 *
 * @since 1.6
 */
public abstract class Filter {

    /**
     * Constructor for subclasses to call.
     */
    protected Filter () {}

    /**
     * A chain of filters associated with a {@link HttpServer}.
     * Each filter in the chain is given one of these so it can invoke the
     * next filter in the chain.
     */
    public static class Chain {

        /**
         * The last element in the chain must invoke the user's
         * handler.
         */
        private ListIterator<Filter> iter;
        private HttpHandler handler;

        /**
         * Creates a {@code Chain} instance with given filters and handler.
         *
         * @param filters the filters that make up the {@code Chain}
         * @param handler the {@link HttpHandler} that will be invoked after
         *                the final {@code Filter} has finished
         */
        public Chain (List<Filter> filters, HttpHandler handler) {
            iter = filters.listIterator();
            this.handler = handler;
        }

        /**
         * Calls the next filter in the chain, or else the users exchange
         * handler, if this is the final filter in the chain. The {@code Filter}
         * may decide to terminate the chain, by not calling this method.
         * In this case, the filter <b>must</b> send the response to the
         * request, because the application's {@linkplain HttpExchange exchange}
         * handler will not be invoked.
         *
         * @param exchange the {@code HttpExchange}
         * @throws IOException if an I/O error occurs
         * @throws NullPointerException if exchange is {@code null}
         */
        public void doFilter (HttpExchange exchange) throws IOException {
            if (!iter.hasNext()) {
                handler.handle (exchange);
            } else {
                Filter f = iter.next();
                f.doFilter (exchange, this);
            }
        }
    }

    /**
     * Asks this filter to pre/post-process the given exchange. The filter
     * can:
     *
     * <ul>
     *     <li> Examine or modify the request headers.
     *     <li> Filter the request body or the response body, by creating suitable
     *     filter streams and calling {@link HttpExchange#setStreams(InputStream, OutputStream)}.
     *     <li> Set attribute objects in the exchange, which other filters or
     *     the exchange handler can access.
     *     <li> Decide to either:
     *
     *     <ol>
     *         <li> Invoke the next filter in the chain, by calling
     *         {@link Filter.Chain#doFilter(HttpExchange)}.
     *         <li> Terminate the chain of invocation, by <b>not</b> calling
     *         {@link Filter.Chain#doFilter(HttpExchange)}.
     *     </ol>
     *
     *     <li> If option 1. above is taken, then when doFilter() returns all subsequent
     *     filters in the Chain have been called, and the response headers can be
     *     examined or modified.
     *     <li> If option 2. above is taken, then this Filter must use the HttpExchange
     *     to send back an appropriate response.
     * </ul>
     *
     * @param exchange the {@code HttpExchange} to be filtered
     * @param chain the {@code Chain} which allows the next filter to be invoked
     * @throws IOException may be thrown by any filter module, and if caught,
     * must be rethrown again
     * @throws NullPointerException if either exchange or chain are {@code null}
     */
    public abstract void doFilter (HttpExchange exchange, Chain chain)
        throws IOException;

    /**
     * Returns a short description of this {@code Filter}.
     *
     * @return a {@code String} describing the {@code Filter}
     */
    public abstract String description ();

    /**
     * Returns a pre-processing {@code Filter} with the given description and
     * operation.
     *
     * <p>The {@link Consumer operation} is the effective implementation of the
     * filter. It is executed for each {@code HttpExchange} before invoking
     * either the next filter in the chain or the exchange handler (if this is
     * the final filter in the chain). Exceptions thrown by the
     * {@code operation} are not handled by the filter.
     *
     * @apiNote
     * A beforeHandler filter is typically used to examine or modify the
     * exchange state before it is handled. The filter {@code operation} is
     * executed before {@link Filter.Chain#doFilter(HttpExchange)} is invoked,
     * so before any subsequent filters in the chain and the exchange handler
     * are executed. The filter {@code operation} is not expected to handle the
     * request or {@linkplain HttpExchange#sendResponseHeaders(int, long) send response headers},
     * since this is commonly done by the exchange handler.
     *
     * <p> Example of adding the {@code "Foo"} response header to all responses:
     * <pre>{@code
     *     var filter = Filter.beforeHandler("Add response header Foo",
     *                 e -> e.getResponseHeaders().set("Foo", "Bar"));
     *     httpContext.getFilters().add(filter);
     * }</pre>
     *
     * @param description the string to be returned from {@link #description()}
     * @param operation the operation of the returned filter
     * @return a filter whose operation is invoked before the exchange is handled
     * @throws NullPointerException if any argument is null
     * @since 17
     */
    public static Filter beforeHandler(String description,
                                       Consumer<HttpExchange> operation) {
        Objects.requireNonNull(description);
        Objects.requireNonNull(operation);
        return new Filter() {
            @Override
            public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                operation.accept(exchange);
                chain.doFilter(exchange);
            }
            @Override
            public String description() {
                return description;
            }
        };
    }

    /**
     * Returns a post-processing {@code Filter} with the given description and
     * operation.
     *
     * <p>The {@link Consumer operation} is the effective implementation of the
     * filter. It is executed for each {@code HttpExchange} after invoking
     * either the next filter in the chain or the exchange handler (if this
     * filter is the final filter in the chain). Exceptions thrown by the
     * {@code operation} are not handled by the filter.
     *
     * @apiNote
     * An afterHandler filter is typically used to examine the exchange state
     * rather than modifying it. The filter {@code operation} is executed after
     * {@link Filter.Chain#doFilter(HttpExchange)} is invoked, this means any
     * subsequent filters in the chain and the exchange handler have been
     * executed. The filter {@code operation} is not expected to handle the
     * exchange or {@linkplain HttpExchange#sendResponseHeaders(int, long) send the response headers}.
     * Doing so is likely to fail, since the exchange has commonly been handled
     * before the {@code operation} is invoked. More specifically, the response
     * may be sent before the filter {@code operation} is executed.
     *
     * <p> Example of adding a filter that logs the response code of all exchanges:
     * <pre>{@code
     *     var filter = Filter.afterHandler("Log response code", e -> log(e.getResponseCode());
     *     httpContext.getFilters().add(filter);
     * }</pre>
     *
     * <p> Example of adding a sequence of afterHandler filters to a context:<br>
     * The order in which the filter operations are invoked is reverse to the
     * order in which the filters are added to the context's filter-list.
     *
     * <pre>{@code
     *     var a1Set = Filter.afterHandler("Set a1", e -> e.setAttribute("a1", "some value"));
     *     var a1Get = Filter.afterHandler("Get a1", e -> doSomething(e.getAttribute("a1")));
     *     httpContext.getFilters().addAll(List.of(a1Get, a1Set));
     * }</pre>
     * <p>The operation of {@code a1Get} will be invoked after the operation of
     * {@code a1Set} because {@code a1Get} was added before {@code a1Set}.
     *
     * @param description the string to be returned from {@link #description()}
     * @param operation the operation of the returned filter
     * @return a filter whose operation is invoked after the exchange is handled
     * @throws NullPointerException if any argument is null
     * @since 17
     */
    public static Filter afterHandler(String description,
                                      Consumer<HttpExchange> operation) {
        Objects.requireNonNull(description);
        Objects.requireNonNull(operation);
        return new Filter() {
            @Override
            public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                chain.doFilter(exchange);
                operation.accept(exchange);
            }
            @Override
            public String description() {
                return description;
            }
        };
    }
}
