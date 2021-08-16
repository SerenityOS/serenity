/*
 * Copyright (c) 1996, 2003, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

/**
 * A {@code ServerCloneException} is thrown if a remote exception occurs
 * during the cloning of a {@code UnicastRemoteObject}.
 *
 * <p>As of release 1.4, this exception has been retrofitted to conform to
 * the general purpose exception-chaining mechanism.  The "nested exception"
 * that may be provided at construction time and accessed via the public
 * {@link #detail} field is now known as the <i>cause</i>, and may be
 * accessed via the {@link Throwable#getCause()} method, as well as
 * the aforementioned "legacy field."
 *
 * <p>Invoking the method {@link Throwable#initCause(Throwable)} on an
 * instance of {@code ServerCloneException} always throws {@link
 * IllegalStateException}.
 *
 * @author  Ann Wollrath
 * @since   1.1
 * @see     java.rmi.server.UnicastRemoteObject#clone()
 */
public class ServerCloneException extends CloneNotSupportedException {

    /**
     * The cause of the exception.
     *
     * <p>This field predates the general-purpose exception chaining facility.
     * The {@link Throwable#getCause()} method is now the preferred means of
     * obtaining this information.
     *
     * @serial
     */
    public Exception detail;

    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = 6617456357664815945L;

    /**
     * Constructs a {@code ServerCloneException} with the specified
     * detail message.
     *
     * @param s the detail message.
     */
    public ServerCloneException(String s) {
        super(s);
        initCause(null);  // Disallow subsequent initCause
    }

    /**
     * Constructs a {@code ServerCloneException} with the specified
     * detail message and cause.
     *
     * @param s the detail message.
     * @param cause the cause
     */
    public ServerCloneException(String s, Exception cause) {
        super(s);
        initCause(null);  // Disallow subsequent initCause
        detail = cause;
    }

    /**
     * Returns the detail message, including the message from the cause, if
     * any, of this exception.
     *
     * @return the detail message
     */
    public String getMessage() {
        if (detail == null)
            return super.getMessage();
        else
            return super.getMessage() +
                "; nested exception is: \n\t" +
                detail.toString();
    }

    /**
     * Returns the cause of this exception.  This method returns the value
     * of the {@link #detail} field.
     *
     * @return  the cause, which may be {@code null}.
     * @since   1.4
     */
    public Throwable getCause() {
        return detail;
    }
}
