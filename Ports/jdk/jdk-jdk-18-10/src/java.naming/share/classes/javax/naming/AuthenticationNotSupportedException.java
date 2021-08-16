/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming;

/**
  * This exception is thrown when
  * the particular flavor of authentication requested is not supported.
  * For example, if the program
  * is attempting to use strong authentication but the directory/naming
  * supports only simple authentication, this exception would be thrown.
  * Identification of a particular flavor of authentication is
  * provider- and server-specific. It may be specified using
  * specific authentication schemes such
  * those identified using SASL, or a generic authentication specifier
  * (such as "simple" and "strong").
  *<p>
  * If the program wants to handle this exception in particular, it
  * should catch AuthenticationNotSupportedException explicitly before
  * attempting to catch NamingException. After catching
  * <code>AuthenticationNotSupportedException</code>, the program could
  * reattempt the authentication using a different authentication flavor
  * by updating the resolved context's environment properties accordingly.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */

public class AuthenticationNotSupportedException extends NamingSecurityException {
    /**
     * Constructs a new instance of AuthenticationNotSupportedException using
     * an explanation. All other fields default to null.
     *
     * @param   explanation     A possibly null string containing additional
     *                          detail about this exception.
     * @see java.lang.Throwable#getMessage
     */
    public AuthenticationNotSupportedException(String explanation) {
        super(explanation);
    }

    /**
      * Constructs a new instance of AuthenticationNotSupportedException
      * with all name resolution fields and explanation initialized to null.
      */
    public AuthenticationNotSupportedException() {
        super();
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -7149033933259492300L;
}
