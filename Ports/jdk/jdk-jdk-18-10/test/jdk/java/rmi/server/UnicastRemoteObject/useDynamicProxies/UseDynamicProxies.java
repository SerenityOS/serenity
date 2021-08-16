/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @build UseDynamicProxies UseDynamicProxies_Stub
 * @run main/othervm/policy=security.policy/timeout=240 UseDynamicProxies true
 * @run main/othervm/policy=security.policy/timeout=240 UseDynamicProxies
 * false
 */

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.rmi.server.RemoteStub;
import java.rmi.server.UnicastRemoteObject;

public class UseDynamicProxies implements RemoteInterface {


    public Object passObject(Object obj) {
        return obj;
    }

    public int passInt(int x) {
        return x;
    }

    public String passString(String string) {
        return string;
    }

    public static void main(String[] args) throws Exception {

        RemoteInterface server = null;
        RemoteInterface proxy = null;

        try {
            System.setProperty("java.rmi.server.ignoreStubClasses", args[0]);
            boolean ignoreStubClasses = Boolean.parseBoolean(args[0]);

            if (System.getSecurityManager() == null) {
                System.setSecurityManager(new SecurityManager());
            }

            System.err.println("export object");
            server = new UseDynamicProxies();
            proxy =
                (RemoteInterface) UnicastRemoteObject.exportObject(server, 0);

            System.err.println("proxy = " + proxy);
            if (ignoreStubClasses) {
                if (!Proxy.isProxyClass(proxy.getClass())) {
                    throw new RuntimeException(
                        "server proxy is not a dynamic proxy");
                }
                if (!(Proxy.getInvocationHandler(proxy) instanceof
                      RemoteObjectInvocationHandler))
                {
                    throw new RuntimeException("invalid invocation handler");
                }

            } else if (!(proxy instanceof RemoteStub)) {
                throw new RuntimeException(
                    "server proxy is not a RemoteStub");
            }

            System.err.println("invoke methods");
            Object obj = proxy.passObject(proxy);
            if (!proxy.equals(obj)) {
                throw new RuntimeException("returned proxy not equal");
            }

            int x = proxy.passInt(53);
            if (x != 53) {
                throw new RuntimeException("returned int not equal");
            }

            String string = proxy.passString("test");
            if (!string.equals("test")) {
                throw new RuntimeException("returned string not equal");
            }

            System.err.println("TEST PASSED");

        } finally {
            if (proxy != null) {
                UnicastRemoteObject.unexportObject(server, true);
            }
        }
    }
}


interface RemoteInterface extends Remote {
    Object passObject(Object obj) throws IOException;
    int passInt(int x) throws IOException;
    String passString(String string) throws IOException;
}
