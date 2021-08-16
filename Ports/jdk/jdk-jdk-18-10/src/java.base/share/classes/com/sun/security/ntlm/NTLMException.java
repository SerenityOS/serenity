/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.ntlm;

import java.security.GeneralSecurityException;

/**
 * An NTLM-related Exception
 */
public final class NTLMException extends GeneralSecurityException {
    @java.io.Serial
    private static final long serialVersionUID = -3298539507906689430L;

    /**
     * If the incoming packet is invalid.
     */
    public static final int PACKET_READ_ERROR = 1;

    /**
     * If the client cannot get a domain value from the server and the
     * caller has not provided one.
     */
    public static final int NO_DOMAIN_INFO = 2;

    /**
     * If the client name is not found on server's user database.
     */
    public static final int USER_UNKNOWN = 3;

    /**
     * If authentication fails.
     */
    public static final int AUTH_FAILED = 4;

    /**
     * If an illegal version string is provided.
     */
    public static final int BAD_VERSION = 5;

    /**
     * Protocol errors.
     */
    public static final int PROTOCOL = 6;

    private int errorCode;

    /**
     * Constructs an NTLMException object.
     * @param errorCode the error code, which can be retrieved by
     * the {@link #errorCode() } method.
     * @param msg the string message, which can be retrived by
     * the {@link Exception#getMessage() } method.
     */
    public NTLMException(int errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code associated with this NTLMException.
     * @return the error code
     */
    public int errorCode() {
        return errorCode;
    }
}
