/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.print;

import java.io.IOException;
import java.io.Serial;

/**
 * The {@code PrinterIOException} class is a subclass of
 * {@link PrinterException} and is used to indicate that an IO error
 * of some sort has occurred while printing.
 *
 * <p>As of release 1.4, this exception has been retrofitted to conform to
 * the general purpose exception-chaining mechanism.  The
 * "{@code IOException} that terminated the print job"
 * that is provided at construction time and accessed via the
 * {@link #getIOException()} method is now known as the <i>cause</i>,
 * and may be accessed via the {@link Throwable#getCause()} method,
 * as well as the aforementioned "legacy method."
 */
public class PrinterIOException extends PrinterException {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 5850870712125932846L;

    /**
     * The IO error that terminated the print job.
     * @serial
     */
    private IOException mException;

    /**
     * Constructs a new {@code PrinterIOException}
     * with the string representation of the specified
     * {@link IOException}.
     * @param exception the specified {@code IOException}
     */
    public PrinterIOException(IOException exception) {
        initCause(null);  // Disallow subsequent initCause
        mException = exception;
    }

    /**
     * Returns the {@code IOException} that terminated
     * the print job.
     *
     * <p>This method predates the general-purpose exception chaining facility.
     * The {@link Throwable#getCause()} method is now the preferred means of
     * obtaining this information.
     *
     * @return the {@code IOException} that terminated
     * the print job.
     * @see IOException
     */
    public IOException getIOException() {
        return mException;
    }

    /**
     * Returns the cause of this exception (the {@code IOException}
     * that terminated the print job).
     *
     * @return  the cause of this exception.
     * @since   1.4
     */
    public Throwable getCause() {
        return mException;
    }
}
