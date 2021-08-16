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

package jdk.internal.net.http.frame;

public abstract class ErrorFrame extends Http2Frame {

    // error codes
    public static final int NO_ERROR = 0x0;
    public static final int PROTOCOL_ERROR = 0x1;
    public static final int INTERNAL_ERROR = 0x2;
    public static final int FLOW_CONTROL_ERROR = 0x3;
    public static final int SETTINGS_TIMEOUT = 0x4;
    public static final int STREAM_CLOSED = 0x5;
    public static final int FRAME_SIZE_ERROR = 0x6;
    public static final int REFUSED_STREAM = 0x7;
    public static final int CANCEL = 0x8;
    public static final int COMPRESSION_ERROR = 0x9;
    public static final int CONNECT_ERROR = 0xa;
    public static final int ENHANCE_YOUR_CALM = 0xb;
    public static final int INADEQUATE_SECURITY = 0xc;
    public static final int HTTP_1_1_REQUIRED = 0xd;
    static final int LAST_ERROR = 0xd;

    static final String[] errorStrings = {
        "Not an error",
        "Protocol error",
        "Internal error",
        "Flow control error",
        "Settings timeout",
        "Stream is closed",
        "Frame size error",
        "Stream not processed",
        "Stream cancelled",
        "Compression state not updated",
        "TCP Connection error on CONNECT",
        "Processing capacity exceeded",
        "Negotiated TLS parameters not acceptable",
        "Use HTTP/1.1 for request"
    };

    public static String stringForCode(int code) {
        if (code < 0) {
            throw new IllegalArgumentException();
        }

        if (code > LAST_ERROR) {
            return "Error: " + Integer.toString(code);
        } else {
            return errorStrings[code];
        }
    }

    int errorCode;

    public ErrorFrame(int streamid, int flags, int errorCode) {
        super(streamid, flags);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return super.toString() + " Error: " + stringForCode(errorCode);
    }

    public int getErrorCode() {
        return this.errorCode;
    }
}
