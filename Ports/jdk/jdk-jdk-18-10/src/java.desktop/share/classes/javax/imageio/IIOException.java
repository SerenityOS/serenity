/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio;

import java.io.IOException;
import java.io.Serial;

/**
 * An exception class used for signaling run-time failure of reading
 * and writing operations.
 *
 * <p> In addition to a message string, a reference to another
 * {@code Throwable} ({@code Error} or
 * {@code Exception}) is maintained.  This reference, if
 * non-{@code null}, refers to the event that caused this
 * exception to occur.  For example, an {@code IOException} while
 * reading from a {@code File} would be stored there.
 *
 */
public class IIOException extends IOException {

    /**
     * Use serialVersionUID from JDK 9 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3216210718638985251L;

    /**
     * Constructs an {@code IIOException} with a given message
     * {@code String}.  No underlying cause is set;
     * {@code getCause} will return {@code null}.
     *
     * @param message the error message.
     *
     * @see #getMessage
     */
    public IIOException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code IIOException} with a given message
     * {@code String} and a {@code Throwable} that was its
     * underlying cause.
     *
     * @param message the error message.
     * @param cause the {@code Throwable} ({@code Error} or
     * {@code Exception}) that caused this exception to occur.
     *
     * @see #getCause
     * @see #getMessage
     */
    public IIOException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
