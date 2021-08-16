/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Map;

import javax.management.MBeanServer;

/**
 * <p>A provider for creating JMX API connector servers using a given
 * protocol.  Instances of this interface are created by {@link
 * JMXConnectorServerFactory} as part of its {@link
 * JMXConnectorServerFactory#newJMXConnectorServer(JMXServiceURL,Map,MBeanServer)
 * newJMXConnectorServer} method.</p>
 *
 * @since 1.5
 */
public interface JMXConnectorServerProvider {
    /**
     * <p>Creates a new connector server at the given address.  Each
     * successful call to this method produces a different
     * <code>JMXConnectorServer</code> object.</p>
     *
     * @param serviceURL the address of the new connector server.  The
     * actual address of the new connector server, as returned by its
     * {@link JMXConnectorServer#getAddress() getAddress} method, will
     * not necessarily be exactly the same.  For example, it might
     * include a port number if the original address did not.
     *
     * @param environment a read-only Map containing named attributes
     * to control the new connector server's behavior.  Keys in this
     * map must be Strings.  The appropriate type of each associated
     * value depends on the attribute.
     *
     * @param mbeanServer the MBean server that this connector server
     * is attached to.  Null if this connector server will be attached
     * to an MBean server by being registered in it.
     *
     * @return a <code>JMXConnectorServer</code> representing the new
     * connector server.  Each successful call to this method produces
     * a different object.
     *
     * @exception NullPointerException if <code>serviceURL</code> or
     * <code>environment</code> is null.
     *
     * @exception IOException It is recommended for a provider
     * implementation to throw {@code MalformedURLException} if the
     * protocol in the {@code serviceURL} is not recognized by this
     * provider, {@code JMXProviderException} if this is a provider
     * for the protocol in {@code serviceURL} but it cannot be used
     * for some reason or any other {@code IOException} if the
     * connector server cannot be created.
     */
    public JMXConnectorServer newJMXConnectorServer(JMXServiceURL serviceURL,
                                                    Map<String,?> environment,
                                                    MBeanServer mbeanServer)
            throws IOException;
}
