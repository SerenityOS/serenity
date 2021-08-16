/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4507539
 * @summary support using dynamic proxies as RMI stubs
 * @author Ann Wollrath
 *
 * @build ToStub ToStub_Stub
 * @run main/othervm/policy=security.policy/timeout=240 ToStub
 */

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.rmi.server.RemoteStub;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.RemoteObject;

public class ToStub implements RemoteInterface {


    public Object passObject(Object obj) {
        return obj;
    }


    public static void main(String[] args) throws Exception {

        RemoteInterface server1 = null;
        RemoteInterface server2 = null;
        RemoteInterface stub = null;
        RemoteInterface proxy = null;

        try {
            System.setProperty("java.rmi.server.ignoreStubClasses", "true");

            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }

            System.err.println("export objects");
            server1 = new ToStub();
            server2 = new ToStub();
            stub = (RemoteInterface) UnicastRemoteObject.exportObject(server1);
            proxy = (RemoteInterface)
                UnicastRemoteObject.exportObject(server2, 0);

            System.err.println("test toStub");
            if (stub != RemoteObject.toStub(server1)) {
                throw new RuntimeException(
                    "toStub returned incorrect value for server1");
            }

            if (!Proxy.isProxyClass(proxy.getClass())) {
                throw new RuntimeException("proxy is not a dynamic proxy");
            }

            if (proxy != RemoteObject.toStub(server2)) {
                throw new RuntimeException(
                    "toStub returned incorrect value for server2");
            }

            try {
                RemoteObject.toStub(new ToStub());
                throw new RuntimeException(
                    "stub returned for exported object!");
            } catch (NoSuchObjectException nsoe) {
            }

            System.err.println("invoke methods");
            Object obj = stub.passObject(stub);
            if (!stub.equals(obj)) {
                throw new RuntimeException("returned stub not equal");
            }

            obj = proxy.passObject(proxy);
            if (!proxy.equals(obj)) {
                throw new RuntimeException("returned proxy not equal");
            }

            System.err.println("TEST PASSED");

        } finally {
            if (stub != null) {
                UnicastRemoteObject.unexportObject(server1, true);
            }

            if (proxy != null) {
                UnicastRemoteObject.unexportObject(server2, true);
            }
        }
    }
}

interface RemoteInterface extends Remote {
    Object passObject(Object obj) throws IOException;
}
