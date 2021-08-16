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

import javax.naming.Name;

/**
 * This exception is thrown when a method
 * does not terminate within the specified time limit.
 * This can happen, for example, if the user specifies that
 * the method should take no longer than 10 seconds, and the
 * method fails to complete with 10 seconds.
 *
 * <p> Synchronization and serialization issues that apply to NamingException
 * apply directly here.
 *
 * @author Rosanna Lee
 * @author Scott Seligman
 *
 * @since 1.3
 */
public class TimeLimitExceededException extends LimitExceededException {
    /**
     * Constructs a new instance of TimeLimitExceededException.
     * All fields default to null.
     */
    public TimeLimitExceededException() {
        super();
    }

    /**
     * Constructs a new instance of TimeLimitExceededException
     * using the argument supplied.
     * @param explanation possibly null detail about this exception.
     * @see java.lang.Throwable#getMessage
     */
    public TimeLimitExceededException(String explanation) {
        super(explanation);
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -3597009011385034696L;
}
