/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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
  * This exception is thrown when resources are not available to complete
  * the requested operation. This might due to a lack of resources on
  * the server or on the client. There are no restrictions to resource types,
  * as different services might make use of different resources. Such
  * restrictions might be due to physical limits and/or administrative quotas.
  * Examples of limited resources are internal buffers, memory, network bandwidth.
  *<p>
  * InsufficientResourcesException is different from LimitExceededException in that
  * the latter is due to user/system specified limits. See LimitExceededException
  * for details.
  * <p>
  * Synchronization and serialization issues that apply to NamingException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */

public class InsufficientResourcesException extends NamingException {
    /**
     * Constructs a new instance of InsufficientResourcesException using an
     * explanation. All other fields default to null.
     *
     * @param   explanation     Possibly null additional detail about this exception.
     * @see java.lang.Throwable#getMessage
     */
    public InsufficientResourcesException(String explanation) {
        super(explanation);
    }

    /**
      * Constructs a new instance of InsufficientResourcesException with
      * all name resolution fields and explanation initialized to null.
      */
    public InsufficientResourcesException() {
        super();
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = 6227672693037844532L;
}
