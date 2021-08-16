/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 5065264
 * @summary Tests that JNDI bind failure doesn't leave an orphan RMI
 * Connector Server object
 * @author Eamonn McManus
 *
 * @run clean JNDIFailureTest
 * @run build JNDIFailureTest
 * @run main JNDIFailureTest
 */

import java.io.IOException;
import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;

public class JNDIFailureTest {
    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        JMXServiceURL jndiUrl =
            new JMXServiceURL("service:jmx:rmi:///jndi/nonexistenthost/x");
        SpyServerImpl impl = new SpyServerImpl();
        JMXConnectorServer cs =
            new RMIConnectorServer(jndiUrl, null, impl, mbs);
        try {
            cs.start();
        } catch (IOException e) {
            e.printStackTrace();
            if (impl.exported) {
                System.out.println("TEST FAILS: server not unexported");
                System.exit(1);
            } else {
                if (cs.isActive()) {
                    System.out.println("TEST FAILS: server still active");
                    System.exit(1);
                }
                System.out.println("Test passed");
                return;
            }
        }
        System.out.println("TEST FAILS: start did not throw exception");
        System.exit(1);
    }

    private static class SpyServerImpl extends RMIJRMPServerImpl {
        SpyServerImpl() throws IOException {
            super(0, null, null, null);
        }

        protected void export() throws IOException {
            super.export();
            exported = true;
        }

        protected void closeServer() throws IOException {
            super.closeServer();
            exported = false;
        }

        boolean exported;
    }
}
