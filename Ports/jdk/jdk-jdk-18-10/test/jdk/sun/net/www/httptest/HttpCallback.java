/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * This interface is implemented by classes that wish to handle incoming HTTP
 * requests and generate responses. This could be a general purpose HTTP server
 * or a test case that expects specific requests from a client.
 * <p>
 * The incoming request fields can be examined via the {@link HttpTransaction}
 * object, and a response can also be generated and sent via the request object.
 */
public interface HttpCallback {
    /**
     * handle the given request and generate an appropriate response.
     * @param msg the transaction containing the request from the
     *        client and used to send the response
     */
    void request (HttpTransaction msg);
}
