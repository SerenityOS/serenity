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
 * @bug 4924577
 * @summary When the current RMIFailureHandler (if any) is invoked
 * because of a server socket accept failure, if it returns false,
 * then (in addition to the accept loop terminating) the associated
 * server socket should be closed.  The server socket should also be
 * closed if the accept loop terminates because of an unexpected
 * exception for which it doesn't even consult the RMIFailureHandler.
 * @author Peter Jones
 *
 * @run main/othervm CloseServerSocketOnTermination
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CloseServerSocketOnTermination {

    private static long TIMEOUT = 5000;

    public static void main(String[] args) throws Exception {
        System.err.println("\nRegression test for bug 4924577\n");

        RMISocketFactory.setFailureHandler(new RMIFailureHandler() {
            public boolean failure(Exception e) { return false; }
        });

        tryWith(new IOException());
        tryWith(new NullPointerException());
        tryWith(new OutOfMemoryError());
        tryWith(new NoClassDefFoundError());
        tryWith(new InternalError());
        tryWith(new Throwable());

        System.err.println("TEST PASSED");
    }

    private static void tryWith(Throwable t) throws Exception {
        Remote impl = new Remote() { };
        try {
            CountDownLatch latch = new CountDownLatch(1);
            UnicastRemoteObject.exportObject(impl, 0, null, new SSF(t, latch));
            if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new Error("server socket not closed");
            }
        } finally {
            UnicastRemoteObject.unexportObject(impl, true);
        }
    }

    private static class SSF implements RMIServerSocketFactory {
        private final Throwable acceptFailure;
        private final CountDownLatch closedLatch;
        SSF(Throwable acceptFailure, CountDownLatch closedLatch) {
            this.acceptFailure = acceptFailure;
            this.closedLatch = closedLatch;
        }
        public ServerSocket createServerSocket(int port) throws IOException {
            return new ServerSocket(port) {
                private int acceptInvocations = 0;
                public synchronized Socket accept() throws IOException {
                    if (acceptInvocations++ == 0) {
                        throwException(acceptFailure);
                    }
                    return super.accept();
                }
                public void close() throws IOException {
                    closedLatch.countDown();
                    super.close();
                }
            };
        }

        // hack to throw an arbitrary (possibly checked) Throwable
        private static void throwException(Throwable t) {
            try {
                toThrow.set(t);
                Thrower.class.newInstance();
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            } catch (InstantiationException e) {
                throw new AssertionError();
            } finally {
                toThrow.remove();
            }
        }
        private static ThreadLocal<Throwable> toThrow =
            new ThreadLocal<Throwable>();
        private static class Thrower {
            Thrower() throws Throwable { throw toThrow.get(); }
        }
    }
}
