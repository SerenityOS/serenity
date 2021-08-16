/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>A provider for creating JMX API connector clients using a given
 * protocol.  Instances of this interface are created by {@link
 * JMXConnectorFactory} as part of its {@link
 * JMXConnectorFactory#newJMXConnector(JMXServiceURL, Map)
 * newJMXConnector} method.</p>
 *
 * @since 1.5
 */
public interface JMXConnectorProvider {
    /**
     * <p>Creates a new connector client that is ready to connect
     * to the connector server at the given address.  Each successful
     * call to this method produces a different
     * <code>JMXConnector</code> object.</p>
     *
     * @param serviceURL the address of the connector server to connect to.
     *
     * @param environment a read-only Map containing named attributes
     * to determine how the connection is made.  Keys in this map must
     * be Strings.  The appropriate type of each associated value
     * depends on the attribute.
     *
     * @return a <code>JMXConnector</code> representing the new
     * connector client.  Each successful call to this method produces
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
     * connection cannot be made because of a communication problem.
     */
    public JMXConnector newJMXConnector(JMXServiceURL serviceURL,
                                        Map<String,?> environment)
            throws IOException;
}
