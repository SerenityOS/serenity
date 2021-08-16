/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

import java.security.GeneralSecurityException;

/**
 * An exception indicating one of a variety of problems retrieving
 * certificates and CRLs from a {@code CertStore}.
 * <p>
 * A {@code CertStoreException} provides support for wrapping
 * exceptions. The {@link #getCause getCause} method returns the throwable,
 * if any, that caused this exception to be thrown.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CertStore
 *
 * @since       1.4
 * @author      Sean Mullan
 */
public class CertStoreException extends GeneralSecurityException {

    @java.io.Serial
    private static final long serialVersionUID = 2395296107471573245L;

    /**
     * Creates a {@code CertStoreException} with {@code null} as
     * its detail message.
     */
    public CertStoreException() {
        super();
    }

    /**
     * Creates a {@code CertStoreException} with the given detail
     * message. A detail message is a {@code String} that describes this
     * particular exception.
     *
     * @param msg the detail message
     */
    public CertStoreException(String msg) {
        super(msg);
    }

    /**
     * Creates a {@code CertStoreException} that wraps the specified
     * throwable. This allows any exception to be converted into a
     * {@code CertStoreException}, while retaining information about the
     * cause, which may be useful for debugging. The detail message is
     * set to ({@code cause==null ? null : cause.toString()}) (which
     * typically contains the class and detail message of cause).
     *
     * @param cause the cause (which is saved for later retrieval by the
     * {@link #getCause getCause()} method). (A {@code null} value is
     * permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public CertStoreException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a {@code CertStoreException} with the specified detail
     * message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the
     * {@link #getCause getCause()} method). (A {@code null} value is
     * permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public CertStoreException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
