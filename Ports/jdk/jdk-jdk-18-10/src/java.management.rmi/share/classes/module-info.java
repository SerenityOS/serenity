/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the {@linkplain javax.management.remote.rmi RMI connector}
 * for the Java Management Extensions (JMX) Remote API.
 *
 * <dl class="notes">
 * <dt>Providers:</dt>
 * <dd>This module provides the
 * {@link javax.management.remote.JMXConnectorProvider} service,
 * which creates JMX connector clients using the RMI protocol.
 * Instances of {@code JMXConnector} can be obtained via the
 * {@link javax.management.remote.JMXConnectorFactory#newJMXConnector
 * JMXConnectorFactory.newJMXConnector} factory method.
 * It also provides the {@link javax.management.remote.JMXConnectorServerProvider} service,
 * which creates JMX connector servers using the RMI protocol.
 * Instances of {@code JMXConnectorServer} can be obtained via the
 * {@link javax.management.remote.JMXConnectorServerFactory#newJMXConnectorServer
 * JMXConnectorServerFactory.newJMXConnectorServer} factory method.
 * </dd>
 * </dl>
 *
 * @provides javax.management.remote.JMXConnectorProvider
 * @provides javax.management.remote.JMXConnectorServerProvider
 *
 * @moduleGraph
 * @since 9
 */
module java.management.rmi {

    requires java.naming;

    requires transitive java.management;
    requires transitive java.rmi;

    exports javax.management.remote.rmi;

    // The qualified export below is required to preserve backward
    // compatibility for the legacy case where an ordered list
    // of package prefixes can be specified to the factory.
    exports com.sun.jmx.remote.protocol.rmi to java.management;

    // jdk.management.agent needs to create an RMIExporter instance.
    exports com.sun.jmx.remote.internal.rmi to jdk.management.agent;

    // The java.management.rmi module provides implementations
    // of the JMXConnectorProvider and JMXConnectorServerProvider
    // services supporting the RMI protocol.
    provides javax.management.remote.JMXConnectorProvider with
        com.sun.jmx.remote.protocol.rmi.ClientProvider;
    provides javax.management.remote.JMXConnectorServerProvider with
        com.sun.jmx.remote.protocol.rmi.ServerProvider;

}
