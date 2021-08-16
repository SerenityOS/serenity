/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package provider;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.JMXProviderException;

public class JMXConnectorProviderImpl implements JMXConnectorProvider {
    private static boolean called = false;
    public static boolean called() {
        return called;
    }

    public JMXConnector newJMXConnector(JMXServiceURL url,
                                        Map<String,?> map)
        throws IOException {
        final String protocol = url.getProtocol();
        called = true;
        System.out.println("JMXConnectorProviderImpl called");

        if(protocol.equals("rmi"))
            return new RMIConnector(url, map);
        if(protocol.equals("throw-provider-exception"))
            throw new JMXProviderException("I have been asked to throw");

        throw new IllegalArgumentException("UNKNOWN PROTOCOL");
    }
}
