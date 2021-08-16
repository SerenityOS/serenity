/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4017232 8046339
 * @summary If, after returning a reference to a remote object in the current
 * VM (which gets implicitly converted to a remote stub), the client fails to
 * both send a DGC dirty call and to send a "DGC acknowledgment", the RMI
 * runtime should eventually allow the remote object to be garbage collected,
 * rather than pinning it indefinitely.
 * @author Peter Jones
 *
 * @modules java.rmi/sun.rmi.transport:+open
 * @build DGCAckFailure DGCAckFailure_Stub
 * @run main/othervm DGCAckFailure
 */

import java.io.*;
import java.net.*;
import java.lang.reflect.Field;
import java.lang.ref.*;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Map;

import sun.rmi.transport.DGCAckHandler;

interface ReturnRemote extends Remote {
    Object returnRemote() throws RemoteException;
}

public class DGCAckFailure implements ReturnRemote {

    private static final long TIMEOUT = 20000;
    private static final long ACK_TIMEOUT = TIMEOUT / 2;

    public Object returnRemote() {
        return new Wrapper(this);
    }

    public static void main(String[] args) throws Exception {

        System.setProperty("sun.rmi.dgc.ackTimeout",
                Long.toString(ACK_TIMEOUT));

        /*
         * Set a socket factory that has a hook for shutting down all client
         * output (writes from client-created sockets and new connection
         * attempts).  We then use this hook right before a remote stub gets
         * deserialized, so that the client will not be able to send a DGC
         * dirty call, or a DGC acknowledgment.  Without the DGC ack, we
         * hope that the RMI runtime will still eventually allow the remote
         * object to be garbage collected.
         */
        RMISocketFactory.setSocketFactory(new TestSF());
        System.err.println("test socket factory set");

        Remote impl = new DGCAckFailure();
        ReferenceQueue refQueue = new ReferenceQueue();
        Reference weakRef = new WeakReference(impl, refQueue);
        ReturnRemote stub =
            (ReturnRemote) UnicastRemoteObject.exportObject(impl);
        System.err.println("remote object exported; stub = " + stub);

        try {
            Object wrappedStub = stub.returnRemote();
            System.err.println("invocation returned: " + wrappedStub);

            impl = null;
            stub = null;        // in case 4114579 ever gets fixed
            System.err.println("strong references to impl cleared");

            System.err.println("waiting for weak reference notification:");
            Reference ref = null;
            for (int i = 0; i < 6; i++) {
                System.gc();
                ref = refQueue.remove(TIMEOUT / 5);
                if (ref != null) {
                    break;
                }
            }
            if (ref != weakRef) {
                throw new RuntimeException("TEST FAILED: " +
                    "timed out, remote object not garbage collected");
            }

            // 8046339
            // All DGCAckHandlers must be properly released after timeout
            Thread.sleep(ACK_TIMEOUT + 100);
            try {
                Field field =
                        DGCAckHandler.class.getDeclaredField("idTable");
                field.setAccessible(true);
                Object obj = field.get(null);
                Map<?,?> idTable = (Map<?,?>)obj;

                if (!idTable.isEmpty()) {
                    throw new RuntimeException("TEST FAILED: " +
                            "DGCAckHandler.idTable isn't empty");
                }
            } catch (ReflectiveOperationException roe) {
                throw new RuntimeException(roe);
            }

            System.err.println("TEST PASSED");

        } finally {
            try {
                UnicastRemoteObject.unexportObject((Remote) weakRef.get(),
                                                   true);
            } catch (Exception e) {
            }
        }
    }

    private static class Wrapper implements Serializable {
        private final Remote obj;
        Wrapper(Remote obj) { this.obj = obj; }

        private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
            TestSF.shutdownClientOutput();
            System.err.println(
                "Wrapper.readObject: SHUTTING DOWN CLIENT OUTPUT");
            in.defaultReadObject();
        }

        public String toString() { return "Wrapper[" + obj + "]"; }
    }

    private static class TestSF extends RMISocketFactory {

        private static volatile boolean shutdown = false;
        static void shutdownClientOutput() { shutdown = true; }

        public Socket createSocket(String host, int port) throws IOException {
            if (shutdown) {
                IOException e = new java.net.ConnectException(
                    "test socket factory rejecting client connection");
                System.err.println(e);
//              e.printStackTrace();
                throw e;
            } else {
                return new TestSocket(host, port);
            }
        }

        public ServerSocket createServerSocket(int port) throws IOException {
            return new ServerSocket(port);
        }

        private static class TestSocket extends Socket {
            TestSocket(String host, int port) throws IOException {
                super(host, port);
            }
            public OutputStream getOutputStream() throws IOException {
                return new TestOutputStream(super.getOutputStream());
            }
        }

        private static class TestOutputStream extends FilterOutputStream {
            TestOutputStream(OutputStream out) { super(out); }
            public void write(int b) throws IOException {
                if (shutdown) {
                    IOException e = new IOException(
                        "connection broken by test socket factory");
                    System.err.println(e);
//                  e.printStackTrace();
                    throw e;
                } else {
                    super.write(b);
                }
            }
        }
    }
}
