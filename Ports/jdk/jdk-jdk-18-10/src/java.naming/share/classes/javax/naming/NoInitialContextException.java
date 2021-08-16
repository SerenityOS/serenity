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
  * This exception is thrown when no initial context implementation
  * can be created.  The policy of how an initial context implementation
  * is selected is described in the documentation of the InitialContext class.
  *<p>
  * This exception can be thrown during any interaction with the
  * InitialContext, not only when the InitialContext is constructed.
  * For example, the implementation of the initial context might lazily
  * retrieve the context only when actual methods are invoked on it.
  * The application should not have any dependency on when the existence
  * of an initial context is determined.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see InitialContext
  * @see javax.naming.directory.InitialDirContext
  * @see javax.naming.spi.NamingManager#getInitialContext
  * @see javax.naming.spi.NamingManager#setInitialContextFactoryBuilder
  * @since 1.3
  */
public class NoInitialContextException extends NamingException {
    /**
      * Constructs an instance of NoInitialContextException.
      * All fields are initialized to null.
      */
    public NoInitialContextException() {
        super();
    }

    /**
      * Constructs an instance of NoInitialContextException with an
      * explanation. All other fields are initialized to null.
      * @param  explanation     Possibly null additional detail about this exception.
      * @see java.lang.Throwable#getMessage
      */
    public NoInitialContextException(String explanation) {
        super(explanation);
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -3413733186901258623L;
}
