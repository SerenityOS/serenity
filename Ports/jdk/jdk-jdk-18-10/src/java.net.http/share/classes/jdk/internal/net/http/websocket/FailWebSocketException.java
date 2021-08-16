/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.websocket;

import static jdk.internal.net.http.websocket.StatusCodes.PROTOCOL_ERROR;

/*
 * Used as a marker for protocol issues in the incoming data, so that the
 * implementation could "Fail the WebSocket Connection" with a status code in
 * the Close message that fits the situation the most.
 *
 *     https://tools.ietf.org/html/rfc6455#section-7.1.7
 */
final class FailWebSocketException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int statusCode;

    FailWebSocketException(String detail) {
        this(detail, PROTOCOL_ERROR);
    }

    FailWebSocketException(String detail, int statusCode) {
        super(detail);
        this.statusCode = statusCode;
    }

    int getStatusCode() {
        return statusCode;
    }

    @Override
    public FailWebSocketException initCause(Throwable cause) {
        return (FailWebSocketException) super.initCause(cause);
    }
}
