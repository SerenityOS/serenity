/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.net;

import java.net.InetAddress;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Defines static methods to be invoked prior to binding or connecting TCP sockets.
 */

public final class NetHooks {

    /**
     * A provider with hooks to allow sockets be converted prior to binding or
     * connecting a TCP socket.
     *
     * <p> Concrete implementations of this class should define a zero-argument
     * constructor and implement the abstract methods specified below.
     */
    public abstract static class Provider {
        /**
         * Initializes a new instance of this class.
         */
        protected Provider() {}

        /**
         * Invoked prior to binding a TCP socket.
         */
        public abstract void implBeforeTcpBind(FileDescriptor fdObj,
                                               InetAddress address,
                                               int port)
            throws IOException;

        /**
         * Invoked prior to connecting an unbound TCP socket.
         */
        public abstract void implBeforeTcpConnect(FileDescriptor fdObj,
                                                 InetAddress address,
                                                 int port)
            throws IOException;
    }

    /**
     * For now, we load the SDP provider on Solaris. In the future this may
     * be changed to use the ServiceLoader facility to allow the deployment of
     * other providers.
     */
    private static final Provider provider = new sun.net.sdp.SdpProvider();

    /**
     * Invoke prior to binding a TCP socket.
     */
    public static void beforeTcpBind(FileDescriptor fdObj,
                                     InetAddress address,
                                     int port)
        throws IOException
    {
        provider.implBeforeTcpBind(fdObj, address, port);
    }

    /**
     * Invoke prior to connecting an unbound TCP socket.
     */
    public static void beforeTcpConnect(FileDescriptor fdObj,
                                        InetAddress address,
                                        int port)
        throws IOException
    {
        provider.implBeforeTcpConnect(fdObj, address, port);
    }
}
