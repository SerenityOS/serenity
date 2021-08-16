/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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


package javax.management.remote;

import java.io.IOException;

// imports for javadoc
import javax.management.MBeanServer;

/**
 * Exception thrown as the result of a remote {@link MBeanServer}
 * method invocation when an <code>Error</code> is thrown while
 * processing the invocation in the remote MBean server.  A
 * <code>JMXServerErrorException</code> instance contains the original
 * <code>Error</code> that occurred as its cause.
 *
 * @see java.rmi.ServerError
 * @since 1.5
 */
public class JMXServerErrorException extends IOException {

    private static final long serialVersionUID = 3996732239558744666L;

    /**
     * Constructs a <code>JMXServerErrorException</code> with the specified
     * detail message and nested error.
     *
     * @param s the detail message.
     * @param err the nested error.  An instance of this class can be
     * constructed where this parameter is null, but the standard
     * connectors will never do so.
     */
    public JMXServerErrorException(String s, Error err) {
        super(s);
        cause = err;
    }

    public Throwable getCause() {
        return cause;
    }

    /**
     * @serial An {@link Error} that caused this exception to be thrown.
     * @see #getCause()
     **/
    private final Error cause;
}
