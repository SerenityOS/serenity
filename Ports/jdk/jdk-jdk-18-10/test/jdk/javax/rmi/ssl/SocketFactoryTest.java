/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4932837 6582235
 * @summary Test SslRMI[Client|Server]SocketFactory equals() and hashCode().
 *          This test does not affect VM global state, so othervm is
 *          not required.
 * @author Daniel Fuchs
 *
 * @run main SocketFactoryTest
 */

import java.io.PrintWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.HashMap;
import javax.net.ssl.SSLContext;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

public class SocketFactoryTest {

    public static final String[] ciphersuite =
        new String[] { "SSL_RSA_WITH_NULL_MD5" };

    public static final String[] protocol =
        new String[] { "TLSv1" };

    public static class MySslRMIClientSocketFactory
        extends SslRMIClientSocketFactory {
    }

    public static class MySslRMIServerSocketFactory
        extends SslRMIServerSocketFactory {
        public MySslRMIServerSocketFactory() {
            super();
        }
        public MySslRMIServerSocketFactory(String[] ciphers,
                                           String[] protocols,
                                           boolean need) {
            super(ciphers, protocols, need);
        }
        public MySslRMIServerSocketFactory(SSLContext context,
                                           String[] ciphers,
                                           String[] protocols,
                                           boolean need) {
            super(context, ciphers, protocols, need);
        }
    }

    public static Object serializeAndClone(Object o) throws Exception {
        System.out.println("Serializing object: " + o);
        final ByteArrayOutputStream obytes =
            new ByteArrayOutputStream();
        final ObjectOutputStream ostr =
            new ObjectOutputStream(obytes);
        ostr.writeObject(o);
        ostr.flush();

        System.out.println("Deserializing object");
        final ByteArrayInputStream ibytes =
            new ByteArrayInputStream(obytes.toByteArray());
        final ObjectInputStream istr =
            new ObjectInputStream(ibytes);
        return istr.readObject();
    }

    public static void testEquals(Object a, Object b, boolean expected) {
        final boolean found = a.equals(b);
        if (found != expected)
            throw new RuntimeException("testEquals failed: objects are " +
                                       ((found)?"equals":"not equals"));
        if (found && a.hashCode()!=b.hashCode())
            throw new RuntimeException("testEquals failed: objects are " +
                                       "equals but their hashcode differ");
    }

    public static void main(String[] args) {
        try {
            System.out.println("SocketFactoryTest START.");

            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, null, null);

            final SslRMIClientSocketFactory client1 =
                new SslRMIClientSocketFactory();
            final SslRMIClientSocketFactory client2 =
                new SslRMIClientSocketFactory();
            final SslRMIClientSocketFactory client11 =
                (SslRMIClientSocketFactory) serializeAndClone(client1);
            final SslRMIClientSocketFactory client21 =
                (SslRMIClientSocketFactory) serializeAndClone(client2);
            final SslRMIServerSocketFactory server1 =
                new SslRMIServerSocketFactory();
            final SslRMIServerSocketFactory server2 =
                new SslRMIServerSocketFactory(null,
                                              null,
                                              false);
            final SslRMIServerSocketFactory server3 =
                new SslRMIServerSocketFactory(ciphersuite,
                                              null,
                                              false);
            final SslRMIServerSocketFactory server4 =
                new SslRMIServerSocketFactory(null,
                                              protocol,
                                              false);
            final SslRMIServerSocketFactory server5 =
                new SslRMIServerSocketFactory(null,
                                              null,
                                              true);
            final SslRMIServerSocketFactory server6 =
                new SslRMIServerSocketFactory(null,
                                              null,
                                              null,
                                              false);
            final SslRMIServerSocketFactory server7 =
                new SslRMIServerSocketFactory(SSLContext.getDefault(),
                                              null,
                                              null,
                                              false);
            final SslRMIServerSocketFactory server8 =
                new SslRMIServerSocketFactory(null,
                                              null,
                                              null,
                                              true);
            final SslRMIServerSocketFactory server9 =
                new SslRMIServerSocketFactory(SSLContext.getDefault(),
                                              null,
                                              null,
                                              true);
            final SslRMIServerSocketFactory server10 =
                new SslRMIServerSocketFactory(context,
                                              null,
                                              null,
                                              true);
            final MySslRMIClientSocketFactory subclient1 =
                new MySslRMIClientSocketFactory();
            final MySslRMIClientSocketFactory subclient2 =
                new MySslRMIClientSocketFactory();
            final MySslRMIServerSocketFactory subserver1 =
                new MySslRMIServerSocketFactory();
            final MySslRMIServerSocketFactory subserver2 =
                new MySslRMIServerSocketFactory(null,
                                                null,
                                                false);
            final MySslRMIServerSocketFactory subserver3 =
                new MySslRMIServerSocketFactory(ciphersuite,
                                                null,
                                                false);
            final MySslRMIServerSocketFactory subserver4 =
                new MySslRMIServerSocketFactory(null,
                                                protocol,
                                                false);
            final MySslRMIServerSocketFactory subserver5 =
                new MySslRMIServerSocketFactory(null,
                                                null,
                                                true);
            final MySslRMIServerSocketFactory subserver6 =
                new MySslRMIServerSocketFactory(null,
                                                ciphersuite,
                                                null,
                                                false);
            final MySslRMIServerSocketFactory subserver7 =
                new MySslRMIServerSocketFactory(SSLContext.getDefault(),
                                                ciphersuite,
                                                null,
                                                false);
            final MySslRMIServerSocketFactory subserver8 =
                new MySslRMIServerSocketFactory(context,
                                                null,
                                                null,
                                                true);

            // clients
            System.out.println("testEquals(client1,client1,true)");
            testEquals(client1,client1,true);
            System.out.println("testEquals(client1,client2,true)");
            testEquals(client1,client2,true);
            System.out.println("testEquals(client1,client11,true)");
            testEquals(client1,client11,true);
            System.out.println("testEquals(client1,client21,true)");
            testEquals(client1,client21,true);
            System.out.println("testEquals(client11,client21,true)");
            testEquals(client11,client21,true);
            System.out.println("testEquals(client1,null,false)");
            testEquals(client1,null,false);
            System.out.println("testEquals(client1,server1,false)");
            testEquals(client1,server1,false);
            System.out.println("testEquals(client21,server2,false)");
            testEquals(client21,server2,false);
            System.out.println("testEquals(client1,new Object(),false)");
            testEquals(client1,new Object(),false);

            // servers
            System.out.println("testEquals(server1,server1,true)");
            testEquals(server1,server1,true);
            System.out.println("testEquals(server2,server2,true)");
            testEquals(server2,server2,true);
            System.out.println("testEquals(server3,server3,true)");
            testEquals(server3,server3,true);
            System.out.println("testEquals(server4,server4,true)");
            testEquals(server4,server4,true);
            System.out.println("testEquals(server5,server5,true)");
            testEquals(server5,server5,true);
            System.out.println("testEquals(server1,server2,true)");
            testEquals(server1,server2,true);
            System.out.println("testEquals(server1,server3,false)");
            testEquals(server1,server3,false);
            System.out.println("testEquals(server2,server3,false)");
            testEquals(server2,server3,false);
            System.out.println("testEquals(server3,server4,false)");
            testEquals(server3,server4,false);
            System.out.println("testEquals(server4,server5,false)");
            testEquals(server4,server5,false);
            System.out.println("testEquals(server6,server7,false)");
            testEquals(server6,server7,false);
            System.out.println("testEquals(server8,server9,false)");
            testEquals(server8,server9,false);
            System.out.println("testEquals(server9,server10,false)");
            testEquals(server9,server10,false);
            System.out.println("testEquals(server2,server6,true)");
            testEquals(server2,server6,true);
            System.out.println("testEquals(server2,server7,false)");
            testEquals(server2,server7,false);
            System.out.println("testEquals(server5,server8,true)");
            testEquals(server5,server8,true);
            System.out.println("testEquals(server5,server9,false)");
            testEquals(server5,server9,false);
            System.out.println("testEquals(server1,null,false)");
            testEquals(server1,null,false);
            System.out.println("testEquals(server2,null,false)");
            testEquals(server2,null,false);
            System.out.println("testEquals(server3,null,false)");
            testEquals(server3,null,false);
            System.out.println("testEquals(server1,client2,false)");
            testEquals(server1,client2,false);
            System.out.println("testEquals(server2,client11,false)");
            testEquals(server2,client11,false);
            System.out.println("testEquals(server1,new Object(),false)");
            testEquals(server1,new Object(),false);

            // client subclass
            System.out.println("testEquals(subclient1,subclient1,true)");
            testEquals(subclient1,subclient1,true);
            System.out.println("testEquals(subclient1,subclient2,true)");
            testEquals(subclient1,subclient2,true);
            System.out.println("testEquals(subclient1,client1,false)");
            testEquals(subclient1,client1,false);
            System.out.println("testEquals(client1,subclient1,false)");
            testEquals(client1,subclient1,false);
            System.out.println("testEquals(subclient1,null,false)");
            testEquals(subclient1,null,false);
            System.out.println("testEquals(subclient1,server1,false)");
            testEquals(subclient1,server1,false);
            System.out.println("testEquals(server1,subclient1,false)");
            testEquals(server1,subclient1,false);
            System.out.println("testEquals(subclient2,subserver2,false)");
            testEquals(subclient1,subserver2,false);
            System.out.println("testEquals(subclient1,new Object(),false)");
            testEquals(subclient1,new Object(),false);

            // server subclass
            System.out.println("testEquals(subserver1,subserver1,true)");
            testEquals(subserver1,subserver1,true);
            System.out.println("testEquals(subserver2,subserver2,true)");
            testEquals(subserver2,subserver2,true);
            System.out.println("testEquals(subserver3,subserver3,true)");
            testEquals(subserver3,subserver3,true);
            System.out.println("testEquals(subserver4,subserver4,true)");
            testEquals(subserver4,subserver4,true);
            System.out.println("testEquals(subserver5,subserver5,true)");
            testEquals(subserver5,subserver5,true);
            System.out.println("testEquals(subserver1,subserver2,true)");
            testEquals(subserver1,subserver2,true);
            System.out.println("testEquals(subserver1,subserver3,false)");
            testEquals(subserver1,subserver3,false);
            System.out.println("testEquals(subserver2,subserver3,false)");
            testEquals(subserver2,subserver3,false);
            System.out.println("testEquals(subserver3,subserver4,false)");
            testEquals(subserver3,subserver4,false);
            System.out.println("testEquals(subserver4,subserver5,false)");
            testEquals(subserver4,subserver5,false);
            System.out.println("testEquals(subserver3,subserver6,true)");
            testEquals(subserver3,subserver6,true);
            System.out.println("testEquals(subserver3,subserver7,false)");
            testEquals(subserver3,subserver7,false);
            System.out.println("testEquals(subserver1,server1,false)");
            testEquals(subserver1,server1,false);
            System.out.println("testEquals(server1,subserver1,false)");
            testEquals(server1,subserver1,false);
            System.out.println("testEquals(subserver2,server2,false)");
            testEquals(subserver2,server2,false);
            System.out.println("testEquals(server2,subserver2,false)");
            testEquals(server2,subserver2,false);
            System.out.println("testEquals(subserver3,server3,false)");
            testEquals(subserver3,server3,false);
            System.out.println("testEquals(server3,subserver3,false)");
            testEquals(server3,subserver3,false);
            System.out.println("testEquals(subserver4,server4,false)");
            testEquals(subserver4,server4,false);
            System.out.println("testEquals(server4,subserver4,false)");
            testEquals(server4,subserver4,false);
            System.out.println("testEquals(subserver5,server5,false)");
            testEquals(subserver5,server5,false);
            System.out.println("testEquals(server5,subserver5,false)");
            testEquals(server5,subserver5,false);
            System.out.println("testEquals(server10,subserver8,false)");
            testEquals(server10,subserver8,false);
            System.out.println("testEquals(subserver8,server10,false)");
            testEquals(subserver8,server10,false);
            System.out.println("testEquals(subserver1,null,false)");
            testEquals(subserver1,null,false);
            System.out.println("testEquals(subserver1,client2,false)");
            testEquals(subserver1,client2,false);
            System.out.println("testEquals(subserver1,subclient2,false)");
            testEquals(subserver1,subclient2,false);
            System.out.println("testEquals(client1,subserver2,false)");
            testEquals(client1,subserver2,false);
            System.out.println("testEquals(subserver1,new Object(),false)");
            testEquals(subserver1,new Object(),false);

            System.out.println("SocketFactoryTest PASSED.");
        } catch (Exception x) {
            System.out.println("SocketFactoryTest FAILED: " + x);
            x.printStackTrace();
            System.exit(1);
        }
    }
}
