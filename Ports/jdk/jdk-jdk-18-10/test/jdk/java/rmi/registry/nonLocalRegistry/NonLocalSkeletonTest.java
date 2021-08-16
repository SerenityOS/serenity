/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.rmi.server.UnicastRef;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;

import java.lang.reflect.InvocationHandler;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Set;


/* @test
 * @bug 8218453
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry:+open java.rmi/sun.rmi.server:+open
 *      java.rmi/sun.rmi.transport:+open java.rmi/sun.rmi.transport.tcp:+open
 * @summary Verify that Registry rejects non-local access for bind, unbind, rebind.
 *    The test is manual because the (non-local) host running rmiregistry must be supplied as a property.
 * @run main/othervm -Dregistry.host=localhost NonLocalSkeletonTest
 */

/*
 * @test
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry:+open java.rmi/sun.rmi.server:+open
 *      java.rmi/sun.rmi.transport:+open java.rmi/sun.rmi.transport.tcp:+open
 * @summary Verify that Registry rejects non-local access for bind, unbind, rebind.
 *    The test is manual because the (non-local) host running rmiregistry must be supplied as a property.
 * @run main/othervm/manual -Dregistry.host=rmi-registry-host NonLocalSkeletonTest
 */

/**
 * Verify that access checks for Registry.bind(), .rebind(), and .unbind()
 * are prevented on remote access to the registry.
 *
 * This test is a manual test and uses a standard rmiregistry running
 * on a *different* host.
 * The test verifies that the access check is performed *before* the object to be
 * bound or rebound is deserialized.
 *
 * Login or ssh to the different host and invoke {@code $JDK_HOME/bin/rmiregistry}.
 * It will not show any output.
 *
 * On the first host modify the @run command above to replace "rmi-registry-host"
 * with the hostname or IP address of the different host and run the test with jtreg.
 */
public class NonLocalSkeletonTest {

    public static void main(String[] args) throws Exception {
        String host = System.getProperty("registry.host");
        if (host == null || host.isEmpty()) {
            throw new RuntimeException("supply a remote host with -Dregistry.host=hostname");
        }

        // Check if running the test on a local system; it only applies to remote
        String myHostName = InetAddress.getLocalHost().getHostName();
        Set<InetAddress> myAddrs = Set.copyOf(Arrays.asList(InetAddress.getAllByName(myHostName)));
        Set<InetAddress> hostAddrs = Set.copyOf(Arrays.asList(InetAddress.getAllByName(host)));
        boolean isLocal = (hostAddrs.stream().anyMatch(i -> myAddrs.contains(i))
                || hostAddrs.stream().anyMatch(h -> h.isLoopbackAddress()));

        int port;
        if (isLocal) {
            // Create a local Registry to use for the test
            port = TestLibrary.getUnusedRandomPort();
            Registry registry = LocateRegistry.createRegistry(port);
            System.out.printf("local registry port: %s%n", registry);
        } else {
            // Use regular rmi registry for non-local test
            port = Registry.REGISTRY_PORT;
        }

        try {

            Registry r = nonStaticRegistryProxy(host, port);

            System.out.printf("RegistryRef: %s%n", r);

            r.rebind("anyRef", r);
            if (!isLocal) {
                throw new RuntimeException("non-local bind should have failed to host: " + host);
            } else {
                System.out.printf("local rebind succeeded%n");
            }
        } catch (RemoteException rex) {
            if (!isLocal) {
                assertIsAccessException(rex);
            } else {
                throw rex;
            }
        }
    }

    /* Returns a non-static proxy for the registry.
     * Follows the form of sun.rmi.server.Util.createProxy.
     * @param implClass the RegistryImpl
     * @param clientRef the registry reference
     **/
    static Registry nonStaticRegistryProxy(String host, int port) {
        final ClassLoader loader = Registry.class.getClassLoader();
        final Class<?>[] interfaces = new Class<?>[]{Registry.class};

        LiveRef liveRef = new LiveRef(new ObjID(ObjID.REGISTRY_ID),
                new TCPEndpoint(host, port, null, null),
                false);

        final InvocationHandler handler = new RemoteObjectInvocationHandler(new UnicastRef(liveRef));

        PrivilegedAction<Registry> action = () -> (Registry) Proxy.newProxyInstance(loader,
                interfaces, handler);
        return AccessController.doPrivileged(action);
    }

    /**
     * Check the exception chain for the expected AccessException and message.
     * @param ex the exception from the remote invocation.
     */
    private static void assertIsAccessException(Throwable ex) {
        Throwable t = ex;
        while (!(t instanceof AccessException) && t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof AccessException) {
            String msg = t.getMessage();
            int asIndex = msg.indexOf("Registry");
            int rrIndex = msg.indexOf("Registry.Registry");     // Obsolete error text
            int disallowIndex = msg.indexOf("disallowed");
            int nonLocalHostIndex = msg.indexOf("non-local host");
            if (asIndex < 0 ||
                    rrIndex != -1 ||
                    disallowIndex < 0 ||
                    nonLocalHostIndex < 0 ) {
                throw new RuntimeException("exception message is malformed", t);
            }
            System.out.printf("Found expected AccessException: %s%n%n", t);
        } else {
            throw new RuntimeException("AccessException did not occur when expected", ex);
        }
    }
}
