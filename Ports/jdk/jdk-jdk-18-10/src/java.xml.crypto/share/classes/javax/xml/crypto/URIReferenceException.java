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
/*
 * $Id: URIReferenceException.java,v 1.4 2005/05/10 15:47:42 mullan Exp $
 */
package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;
import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;

/**
 * Indicates an exceptional condition thrown while dereferencing a
 * {@link URIReference}.
 *
 * <p>A {@code URIReferenceException} can contain a cause: another
 * throwable that caused this {@code URIReferenceException} to get thrown.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see URIDereferencer#dereference(URIReference, XMLCryptoContext)
 * @see RetrievalMethod#dereference(XMLCryptoContext)
 */
public class URIReferenceException extends Exception {

    private static final long serialVersionUID = 7173469703932561419L;

    /**
     * The throwable that caused this exception to get thrown, or null if this
     * exception was not caused by another throwable or if the causative
     * throwable is unknown.
     *
     * @serial
     */
    private Throwable cause;

    /**
     * The {@code URIReference} that was being dereferenced
     * when the exception was thrown, or {@code null} if not specified.
     */
    private URIReference uriReference;

    /**
     * Constructs a new {@code URIReferenceException} with
     * {@code null} as its detail message.
     */
    public URIReferenceException() {
        super();
    }

    /**
     * Constructs a new {@code URIReferenceException} with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public URIReferenceException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code URIReferenceException} with the
     * specified detail message and cause.
     * <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param message the detail message
     * @param cause the cause (A {@code null} value is permitted, and
     *        indicates that the cause is nonexistent or unknown.)
     */
    public URIReferenceException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    /**
     * Constructs a new {@code URIReferenceException} with the
     * specified detail message, cause and {@code URIReference}.
     * <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param message the detail message
     * @param cause the cause (A {@code null} value is permitted, and
     *        indicates that the cause is nonexistent or unknown.)
     * @param uriReference the {@code URIReference} that was being
     *    dereferenced when the error was encountered
     * @throws NullPointerException if {@code uriReference} is
     *    {@code null}
     */
    public URIReferenceException(String message, Throwable cause,
        URIReference uriReference) {
        this(message, cause);
        if (uriReference == null) {
            throw new NullPointerException("uriReference cannot be null");
        }
        this.uriReference = uriReference;
    }

    /**
     * Constructs a new {@code URIReferenceException} with the specified
     * cause and a detail message of {@code (cause==null ? null :
     * cause.toString())} (which typically contains the class and detail
     * message of {@code cause}).
     *
     * @param cause the cause (A {@code null} value is permitted, and
     *        indicates that the cause is nonexistent or unknown.)
     */
    public URIReferenceException(Throwable cause) {
        super(cause==null ? null : cause.toString());
        this.cause = cause;
    }

    /**
     * Returns the {@code URIReference} that was being dereferenced
     * when the exception was thrown.
     *
     * @return the {@code URIReference} that was being dereferenced
     * when the exception was thrown, or {@code null} if not specified
     */
    public URIReference getURIReference() {
        return uriReference;
    }

    /**
     * Returns the cause of this {@code URIReferenceException} or
     * {@code null} if the cause is nonexistent or unknown.  (The
     * cause is the throwable that caused this
     * {@code URIReferenceException} to get thrown.)
     *
     * @return the cause of this {@code URIReferenceException} or
     *    {@code null} if the cause is nonexistent or unknown.
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Prints this {@code URIReferenceException}, its backtrace and
     * the cause's backtrace to the standard error stream.
     */
    public void printStackTrace() {
        super.printStackTrace();
        //XXX print backtrace of cause
    }

    /**
     * Prints this {@code URIReferenceException}, its backtrace and
     * the cause's backtrace to the specified print stream.
     *
     * @param s {@code PrintStream} to use for output
     */
    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        //XXX print backtrace of cause
    }

    /**
     * Prints this {@code URIReferenceException}, its backtrace and
     * the cause's backtrace to the specified print writer.
     *
     * @param s {@code PrintWriter} to use for output
     */
    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
        //XXX print backtrace of cause
    }
}
