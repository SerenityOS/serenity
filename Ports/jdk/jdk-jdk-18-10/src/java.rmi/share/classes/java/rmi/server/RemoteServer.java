/*
 * Copyright (c) 1996, 2002, Oracle and/or its affiliates. All rights reserved.
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

import java.rmi.*;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.runtime.Log;

/**
 * The <code>RemoteServer</code> class is the common superclass to server
 * implementations and provides the framework to support a wide range
 * of remote reference semantics.  Specifically, the functions needed
 * to create and export remote objects (i.e. to make them remotely
 * available) are provided abstractly by <code>RemoteServer</code> and
 * concretely by its subclass(es).
 *
 * @author  Ann Wollrath
 * @since   1.1
 */
public abstract class RemoteServer extends RemoteObject
{
    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = -4100238210092549637L;

    /**
     * Constructs a <code>RemoteServer</code>.
     * @since 1.1
     */
    protected RemoteServer() {
        super();
    }

    /**
     * Constructs a <code>RemoteServer</code> with the given reference type.
     *
     * @param ref the remote reference
     * @since 1.1
     */
    protected RemoteServer(RemoteRef ref) {
        super(ref);
    }

    /**
     * Returns a string representation of the client host for the
     * remote method invocation being processed in the current thread.
     *
     * @return  a string representation of the client host
     *
     * @throws  ServerNotActiveException if no remote method invocation
     * is being processed in the current thread
     *
     * @since   1.1
     */
    public static String getClientHost() throws ServerNotActiveException {
        return sun.rmi.transport.tcp.TCPTransport.getClientHost();
    }

    /**
     * Log RMI calls to the output stream <code>out</code>. If
     * <code>out</code> is <code>null</code>, call logging is turned off.
     *
     * <p>If there is a security manager, its
     * <code>checkPermission</code> method will be invoked with a
     * <code>java.util.logging.LoggingPermission("control")</code>
     * permission; this could result in a <code>SecurityException</code>.
     *
     * @param   out the output stream to which RMI calls should be logged
     * @throws  SecurityException  if there is a security manager and
     *          the invocation of its <code>checkPermission</code> method
     *          fails
     * @see #getLog
     * @since 1.1
     */
    public static void setLog(java.io.OutputStream out)
    {
        logNull = (out == null);
        UnicastServerRef.callLog.setOutputStream(out);
    }

    /**
     * Returns stream for the RMI call log.
     * @return the call log
     * @see #setLog
     * @since 1.1
     */
    public static java.io.PrintStream getLog()
    {
        return (logNull ? null : UnicastServerRef.callLog.getPrintStream());
    }

    // initialize log status
    private static boolean logNull = !UnicastServerRef.logCalls;
}
