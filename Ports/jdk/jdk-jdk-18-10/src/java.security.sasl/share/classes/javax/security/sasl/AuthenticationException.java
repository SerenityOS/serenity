/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.sasl;

/**
 * This exception is thrown by a SASL mechanism implementation
 * to indicate that the SASL
 * exchange has failed due to reasons related to authentication, such as
 * an invalid identity, passphrase, or key.
 * <p>
 * Note that the lack of an AuthenticationException does not mean that
 * the failure was not due to an authentication error.  A SASL mechanism
 * implementation might throw the more general SaslException instead of
 * AuthenticationException if it is unable to determine the nature
 * of the failure, or if does not want to disclose the nature of
 * the failure, for example, due to security reasons.
 *
 * @since 1.5
 *
 * @author Rosanna Lee
 * @author Rob Weltman
 */
public class AuthenticationException extends SaslException {
    /**
     * Constructs a new instance of {@code AuthenticationException}.
     * The root exception and the detailed message are null.
     */
    public AuthenticationException () {
        super();
    }

    /**
     * Constructs a new instance of {@code AuthenticationException}
     * with a detailed message.
     * The root exception is null.
     * @param detail A possibly null string containing details of the exception.
     *
     * @see java.lang.Throwable#getMessage
     */
    public AuthenticationException (String detail) {
        super(detail);
    }

    /**
     * Constructs a new instance of {@code AuthenticationException} with a detailed message
     * and a root exception.
     *
     * @param detail A possibly null string containing details of the exception.
     * @param ex A possibly null root exception that caused this exception.
     *
     * @see java.lang.Throwable#getMessage
     * @see #getCause
     */
    public AuthenticationException (String detail, Throwable ex) {
        super(detail, ex);
    }

    /** Use serialVersionUID from JSR 28 RI for interoperability */
    private static final long serialVersionUID = -3579708765071815007L;
}
