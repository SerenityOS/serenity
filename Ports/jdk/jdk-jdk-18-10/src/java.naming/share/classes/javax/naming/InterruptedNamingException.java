/*
 * Copyright (c) 1999, 2001, Oracle and/or its affiliates. All rights reserved.
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
  * This exception is thrown when the naming operation
  * being invoked has been interrupted. For example, an application
  * might interrupt a thread that is performing a search. If the
  * search supports being interrupted, it will throw
  * InterruptedNamingException. Whether an operation is interruptible
  * and when depends on its implementation (as provided by the
  * service providers). Different implementations have different ways
  * of protecting their resources and objects from being damaged
  * due to unexpected interrupts.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see Context
  * @see javax.naming.directory.DirContext
  * @see java.lang.Thread#interrupt
  * @see java.lang.InterruptedException
  * @since 1.3
  */

public class InterruptedNamingException extends NamingException {
    /**
      * Constructs an instance of InterruptedNamingException using an
      * explanation of the problem.
      * All name resolution-related fields are initialized to null.
      * @param explanation      A possibly null message explaining the problem.
      * @see java.lang.Throwable#getMessage
      */
    public InterruptedNamingException(String explanation) {
        super(explanation);
    }

    /**
      * Constructs an instance of InterruptedNamingException with
      * all name resolution fields and explanation initialized to null.
      */
    public InterruptedNamingException() {
        super();
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = 6404516648893194728L;
}
