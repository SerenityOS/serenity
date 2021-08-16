/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4924683
 * @summary Check RMI/JRMP stubs can be deserialized using user's loader
 * @author Eamonn McManus
 *
 * @run clean DeserializeEncodedURLTest SingleClassLoader
 * @run build DeserializeEncodedURLTest SingleClassLoader
 * @run main DeserializeEncodedURLTest
 */

import java.io.*;
import java.rmi.*;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;

/*
  Test that the RMI connector client can handle a URL of the form
  where the serialized RMIServer stub is encoded directly in the URL,
  when the class of that stub is known to the supplied
  DEFAULT_CLASS_LOADER but not to the calling code's class loader.
  This is an unusual usage, and is not explicitly specified in the JMX
  Remote API, but it is potentially useful where client and server
  agree to a code base for mutant stubs (that e.g. use a different
  protocol or include debugging or optimization).

  We make an RMI connector server by giving it an instance of an
  RMIJRMPServerImpl subclass that manufactures mutant stubs.  These
  stubs are known to a special loader (mutantLoader) but not to this
  test's loader.  We set up the client's default loader to
  mutantLoader, and check that it can deserialize the stub containing
  the mutant stub.

  This test incidentally creates the connector server as an MBean
  rather than using the JMXConnectorServerFactory, just because I'm
  not sure we have coverage of that elsewhere.
*/
public class DeserializeEncodedURLTest {
    private static final ClassLoader mutantLoader =
        new SingleClassLoader("SubMutantRMIServerStub",
                              MutantRMIServerStub.class,
                              MutantRMIServerStub.class.getClassLoader());
    private static final Class subMutantRMIServerStubClass;
    static {
        try {
            subMutantRMIServerStubClass =
                mutantLoader.loadClass("SubMutantRMIServerStub");
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Check that we can deserialize a mutant stub " +
                           "from an RMI connector URL even when the stub's " +
                           "class is known to the user's default loader " +
                           "but not the caller's loader");

        System.out.println("Create RMI connector server as an MBean");

        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        ObjectName csName = new ObjectName("test:type=RMIConnectorServer");
        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        RMIServerImpl impl = new MutantRMIServerImpl();
        mbs.createMBean("javax.management.remote.rmi.RMIConnectorServer",
                        csName,
                        new Object[] {url, null, impl, null},
                        new String[] {JMXServiceURL.class.getName(),
                                      Map.class.getName(),
                                      RMIServerImpl.class.getName(),
                                      MBeanServer.class.getName()});
        mbs.invoke(csName, "start", new Object[0], new String[0]);

        JMXServiceURL address =
            (JMXServiceURL) mbs.getAttribute(csName, "Address");

        System.out.println("Address with mutant stub: " + address);

        Map env = new HashMap();
        env.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER, mutantLoader);
        JMXConnector conn = JMXConnectorFactory.newJMXConnector(address, env);

        System.out.println("Client successfully created with this address");
        System.out.println("Try to connect newly-created client");

        try {
            conn.connect();
            System.out.println("TEST FAILS: Connect worked but should not " +
                               "have");
            System.exit(1);
        } catch (MutantException e) {
            System.out.println("Caught MutantException as expected");
        } catch (Exception e) {
            System.out.println("TEST FAILS: Caught unexpected exception:");
            e.printStackTrace(System.out);
            System.exit(1);
        }

        mbs.invoke(csName, "stop", new Object[0], new String[0]);
        System.out.println("Test passed");
    }

    private static class MutantException extends IOException {}

    public static class MutantRMIServerStub
            implements RMIServer, Serializable {
        public MutantRMIServerStub() {}

        public String getVersion() {
            return "1.0 BOGUS";
        }

        public RMIConnection newClient(Object credentials) throws IOException {
            throw new MutantException();
        }
    }

    private static class MutantRMIServerImpl extends RMIJRMPServerImpl {
        public MutantRMIServerImpl() throws IOException {
            super(0, null, null, null);
        }

        public Remote toStub() throws IOException {
            try {
                return (Remote) subMutantRMIServerStubClass.newInstance();
            } catch (Exception e) {
                IOException ioe =
                    new IOException("Couldn't make submutant stub");
                ioe.initCause(e);
                throw ioe;
            }
        }
    }
}
