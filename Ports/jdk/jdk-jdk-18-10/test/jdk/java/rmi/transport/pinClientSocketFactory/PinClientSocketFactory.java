/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4486732
 * @summary When a remote stub contains a client socket factory and a
 * remote invocation is made using that stub, the factory should not
 * be held strongly reachable by the RMI implementation forever; in
 * particular, after the stub has become unreachable and all
 * connections to its endpoint have been closed, then the factory
 * should become unreachable too (through the RMI implementation).
 * @author Peter Jones
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm -Dsun.rmi.transport.connectionTimeout=2000
 *     PinClientSocketFactory
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PinClientSocketFactory {

    private static final int SESSIONS = 50;

    public interface Factory extends Remote {
        Session getSession() throws RemoteException;
    }

    public interface Session extends Remote {
        void ping() throws RemoteException;
    }

    private static class FactoryImpl implements Factory {
        FactoryImpl() { }
        public Session getSession() throws RemoteException {
            Session impl = new SessionImpl();
            UnicastRemoteObject.exportObject(impl, 0, new CSF(), new SSF());
            // return impl instead of stub to work around 4114579
            return impl;
        }
    }

    private static class SessionImpl implements Session {
        SessionImpl() { }
        public void ping() { }
    }

    public static void main(String[] args) throws Exception {
        System.err.println("\nRegression test for bug 4486732\n");

        Factory factoryImpl = new FactoryImpl();
        Factory factoryStub =
            (Factory) UnicastRemoteObject.exportObject(factoryImpl, 0);
        for (int i = 0; i < SESSIONS; i++) {
            Session session = factoryStub.getSession();
            session.ping();
        }
        UnicastRemoteObject.unexportObject(factoryImpl, true);

        Registry registryImpl = TestLibrary.createRegistryOnEphemeralPort();
        int port = TestLibrary.getRegistryPort(registryImpl);
        System.out.println("Registry listening on port " + port);

        CSF csf = new CSF();
        Reference<CSF> registryRef = new WeakReference<CSF>(csf);
        Registry registryStub = LocateRegistry.getRegistry("", port, csf);
        csf = null;
        registryStub.list();
        registryStub = null;
        UnicastRemoteObject.unexportObject(registryImpl, true);

        System.gc();
        // allow connections to time out
        Thread.sleep(3 * Long.getLong("sun.rmi.transport.connectionTimeout",
                                      15000));
        System.gc();

        if (CSF.deserializedInstances.size() != SESSIONS) {
            throw new Error("unexpected number of deserialized instances: " +
                            CSF.deserializedInstances.size());
        }

        int nonNullCount = 0;
        for (Reference<CSF> ref : CSF.deserializedInstances) {
            csf = ref.get();
            if (csf != null) {
                System.err.println("non-null deserialized instance: " + csf);
                nonNullCount++;
            }
        }
        if (nonNullCount > 0) {
            throw new Error("TEST FAILED: " +
                            nonNullCount + " non-null deserialized instances");
        }

        csf = registryRef.get();
        if (csf != null) {
            System.err.println("non-null registry instance: " + csf);
            throw new Error("TEST FAILED: non-null registry instance");
        }

        System.err.println("TEST PASSED");
    }

    private static class CSF implements RMIClientSocketFactory, Serializable {
        static final List<Reference<CSF>> deserializedInstances =
            Collections.synchronizedList(new ArrayList<Reference<CSF>>());
        private static final AtomicInteger count = new AtomicInteger(0);
        private int num = count.incrementAndGet();
        CSF() { }
        public Socket createSocket(String host, int port) throws IOException {
            return new Socket(host, port);
        }
        public int hashCode() {
            return num;
        }
        public boolean equals(Object obj) {
            return obj instanceof CSF && ((CSF) obj).num == num;
        }
        private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            in.defaultReadObject();
            deserializedInstances.add(new WeakReference<CSF>(this));
        }
    }

    private static class SSF implements RMIServerSocketFactory {
        SSF() { }
        public ServerSocket createServerSocket(int port) throws IOException {
            return new ServerSocket(port);
        }
    }
}
