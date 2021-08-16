/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: TransformException.java,v 1.3 2005/05/10 16:03:48 mullan Exp $
 */
package javax.xml.crypto.dsig;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Indicates an exceptional condition that occurred while executing a
 * transform algorithm.
 *
 * <p>A {@code TransformException} can contain a cause: another
 * throwable that caused this {@code TransformException} to get thrown.
 *
 * @see Transform#transform
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 */
public class TransformException extends Exception {

    private static final long serialVersionUID = 5082634801360427800L;

    /**
     * The throwable that caused this exception to get thrown, or null if this
     * exception was not caused by another throwable or if the causative
     * throwable is unknown.
     *
     * @serial
     */
    private Throwable cause;

    /**
     * Constructs a new {@code TransformException} with
     * {@code null} as its detail message.
     */
    public TransformException() {
        super();
    }

    /**
     * Constructs a new {@code TransformException} with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public TransformException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TransformException} with the
     * specified detail message and cause.
     * <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param message the detail message
     * @param cause the cause (A {@code null} value is permitted, and
     *        indicates that the cause is nonexistent or unknown.)
     */
    public TransformException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    /**
     * Constructs a new {@code TransformException} with the specified
     * cause and a detail message of
     * {@code (cause==null ? null : cause.toString())}
     * (which typically contains the class and detail message of
     * {@code cause}).
     *
     * @param cause the cause (A {@code null} value is permitted, and
     *        indicates that the cause is nonexistent or unknown.)
     */
    public TransformException(Throwable cause) {
        super(cause==null ? null : cause.toString());
        this.cause = cause;
    }

    /**
     * Returns the cause of this {@code TransformException} or
     * {@code null} if the cause is nonexistent or unknown.  (The
     * cause is the throwable that caused this
     * {@code TransformException} to get thrown.)
     *
     * @return the cause of this {@code TransformException} or
     *         {@code null} if the cause is nonexistent or unknown.
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Prints this {@code TransformException}, its backtrace and
     * the cause's backtrace to the standard error stream.
     */
    public void printStackTrace() {
        super.printStackTrace();
        if (cause != null) {
            cause.printStackTrace();
        }
    }

    /**
     * Prints this {@code TransformException}, its backtrace and
     * the cause's backtrace to the specified print stream.
     *
     * @param s {@code PrintStream} to use for output
     */
    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        if (cause != null) {
            cause.printStackTrace(s);
        }
    }

    /**
     * Prints this {@code TransformException}, its backtrace and
     * the cause's backtrace to the specified print writer.
     *
     * @param s {@code PrintWriter} to use for output
     */
    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
        if (cause != null) {
            cause.printStackTrace(s);
        }
    }
}
