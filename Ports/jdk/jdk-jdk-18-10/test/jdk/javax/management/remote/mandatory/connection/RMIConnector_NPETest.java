/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary NPE IN RMIConnector.connect
 * @bug 6984520
 * @library /java/rmi/testlibrary
 * @modules java.management.rmi
 *          java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @run clean RMIConnector_NPETest
 * @run build TestLibrary RMID
 * @run build RMIConnector_NPETest
 * @run main RMIConnector_NPETest
 */
import java.io.IOException;
import javax.management.*;
import javax.management.remote.rmi.*;

public class RMIConnector_NPETest {
    public static void main(String argv[]) throws Exception {
        RMID rmid = RMID.createRMID();
        Exception failureCause = null;
        RMIConnector agent = null;

        try {
            rmid.start();
            int rmidPort = rmid.getPort();
            MBeanServer mbs = MBeanServerFactory.createMBeanServer();
            RMIJRMPServerImpl rmiserver = new RMIJRMPServerImpl(rmidPort, null, null, null);
            rmiserver.setMBeanServer(mbs);
            agent = new RMIConnector(rmiserver, null);
            agent.connect();
        } catch (NullPointerException npe) {
            failureCause = npe;
        } catch (Exception e) {
            // OK
        } finally {
            if (agent != null) {
                try {
                    agent.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            rmid.destroy();
        }

        if (failureCause != null) {
            TestLibrary.bomb("Test failed", failureCause);
        }

    }
}
