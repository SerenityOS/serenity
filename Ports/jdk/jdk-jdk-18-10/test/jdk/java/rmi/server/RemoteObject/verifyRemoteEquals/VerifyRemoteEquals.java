/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4251010
 * @summary equals does not works on stub objects created with
 *           custom socket AndFactory
 * @author Laird Dornin
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm/timeout=40 VerifyRemoteEquals
 */

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * Test ensures that a stub that has never been serialized and one
 * that has can be .equals().  Test also ensures that stubs with
 * custom socket factories can be .equals() with equivalent stubs.
 */
public class VerifyRemoteEquals {

    /**
     * Remote interface.
     */
    public interface Test extends Remote {
    }

    /**
     * Implementation of Remote interface passing custom socket
     * factories
     */
    public static final class TestImpl
        extends UnicastRemoteObject implements Test
    {
        public TestImpl() throws RemoteException {
            super();
        }

        public TestImpl(RMIClientSocketFactory clientFactory,
                        RMIServerSocketFactory serverFactory)
            throws RemoteException
        {

            super(0, clientFactory, serverFactory);
        }

        public TestImpl(RMISocketFactory factory)
            throws RemoteException
        {
            super(0, factory, factory);
        }
    }

    /**
     * Remote interface for retrieving Test object.
     */
    public interface TestHome extends Remote {
        public Test get() throws RemoteException;
    }

    /**
     * Implementation of interface TestHome.
     */
    public static final class TestHomeImpl
        extends UnicastRemoteObject implements TestHome
    {
        private Test test;

        public TestHomeImpl(Test test)
            throws RemoteException {

            super();

            this.test = test;
        }

        public Test get() {
            return test;
        }
    }

    /**
     * Custom server socket factory.
     */
    public static final class ServerSocketAndFactory
        extends ServerSocket implements RMIServerSocketFactory, Serializable
    {
        ServerSocketAndFactory() throws IOException, java.net.UnknownHostException {
            // I am forced to do something useless with the parent
            // constructor
            super(0);
        }
        ServerSocketAndFactory(int port) throws IOException,
            java.net.UnknownHostException
        {
            super(port);
        }

        public ServerSocket createServerSocket(int port)
            throws IOException
        {

            return new ServerSocketAndFactory(port);
        }

        public int hashCode() {
            return getLocalPort();
        }

        public boolean equals(Object obj) {
            if (obj instanceof ServerSocketAndFactory) {
                ServerSocketAndFactory ssf = (ServerSocketAndFactory) obj;
                if (getLocalPort() == ssf.getLocalPort()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Custom socket factory.
     */
    public static final class ClientSocketAndFactory
        extends Socket implements RMIClientSocketFactory, Serializable
    {
        ClientSocketAndFactory() {
        }
        ClientSocketAndFactory(String host, int port) throws IOException {
            super(host, port);
        }

        public Socket createSocket(String host, int port)
            throws IOException {

            return new ClientSocketAndFactory(host, port);
        }

        public int hashCode() {
            return getPort();
        }

        public boolean equals(Object obj) {

            if (obj instanceof ClientSocketAndFactory) {
                ClientSocketAndFactory csf = (ClientSocketAndFactory) obj;
                if (getPort() == csf.getPort()) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void main(String[] args) {

        try {
            System.out.println("\n\nRegression test for, 4251010\n\n");

            Test test = new TestImpl(new ClientSocketAndFactory(),
                                     new ServerSocketAndFactory());
            TestHome home = new TestHomeImpl(test);

            Test test0 = ((Test) RemoteObject.toStub(test));
            Test test1 = ((TestHome) RemoteObject.toStub(home)).get();
            Test test2 = ((TestHome) RemoteObject.toStub(home)).get();
            Test test3 = ((Test) (new MarshalledObject(test)).get());

            if (test0.equals(test1)) {
                System.out.println("test0, test1, stubs equal");
            } else {
                TestLibrary.bomb("test0, test1, stubs not equal");
            }

            if (test1.equals(test2)) {
                System.out.println("test1, test2, stubs equal");
            } else {
                TestLibrary.bomb("test1, test2, stubs not equal");
            }

            // explicitly compare an unmarshalled object with toStub
            // return
            if (test2.equals(test3)) {
                System.out.println("test2, test3, stubs equal");
            } else {
                TestLibrary.bomb("test2, test3, stubs not equal");
            }

            test0 = null;
            test1 = null;
            test2 = null;
            test3 = null;

            TestLibrary.unexport(test);
            TestLibrary.unexport(home);

            System.err.println("test passed: stubs were equal");

        } catch (Exception e) {
            TestLibrary.bomb("test got unexpected exception", e);
        }
    }
}
