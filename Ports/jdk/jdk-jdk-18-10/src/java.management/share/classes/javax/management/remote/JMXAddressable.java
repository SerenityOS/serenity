/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>Implemented by objects that can have a {@code JMXServiceURL} address.
 * All {@link JMXConnectorServer} objects implement this interface.
 * Depending on the connector implementation, a {@link JMXConnector}
 * object may implement this interface too.  {@code JMXConnector}
 * objects for the RMI Connector are instances of
 * {@link javax.management.remote.rmi.RMIConnector RMIConnector} which
 * implements this interface.</p>
 *
 * <p>An object implementing this interface might not have an address
 * at a given moment.  This is indicated by a null return value from
 * {@link #getAddress()}.</p>
 *
 * @since 1.6
 */
public interface JMXAddressable {
    /**
     * <p>The address of this object.</p>
     *
     * @return the address of this object, or null if it
     * does not have one.
     */
    public JMXServiceURL getAddress();
}
