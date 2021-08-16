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
  * This exception is thrown when attempting to destroy a context that
  * is not empty.
  *<p>
  * If the program wants to handle this exception in particular, it
  * should catch ContextNotEmptyException explicitly before attempting to
  * catch NamingException. For example, after catching ContextNotEmptyException,
  * the program might try to remove the contents of the context before
  * reattempting the destroy.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see Context#destroySubcontext
  * @since 1.3
  */
public class ContextNotEmptyException extends NamingException {
    /**
     * Constructs a new instance of ContextNotEmptyException using an
     * explanation. All other fields default to null.
     *
     * @param   explanation     Possibly null string containing
     * additional detail about this exception.
     * @see java.lang.Throwable#getMessage
     */
    public ContextNotEmptyException(String explanation) {
        super(explanation);
    }

    /**
      * Constructs a new instance of ContextNotEmptyException with
      * all name resolution fields and explanation initialized to null.
      */
    public ContextNotEmptyException() {
        super();
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = 1090963683348219877L;
}
