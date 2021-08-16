/*
 * Copyright (c) 1996, 1998, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

/**
 * An <code>ServerNotActiveException</code> is an <code>Exception</code>
 * thrown during a call to <code>RemoteServer.getClientHost</code> if
 * the getClientHost method is called outside of servicing a remote
 * method call.
 *
 * @author  Roger Riggs
 * @since   1.1
 * @see java.rmi.server.RemoteServer#getClientHost()
 */
public class ServerNotActiveException extends java.lang.Exception {

    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = 4687940720827538231L;

    /**
     * Constructs an <code>ServerNotActiveException</code> with no specified
     * detail message.
     * @since 1.1
     */
    public ServerNotActiveException() {}

    /**
     * Constructs an <code>ServerNotActiveException</code> with the specified
     * detail message.
     *
     * @param s the detail message.
     * @since 1.1
     */
    public ServerNotActiveException(String s)
    {
        super(s);
    }
}
