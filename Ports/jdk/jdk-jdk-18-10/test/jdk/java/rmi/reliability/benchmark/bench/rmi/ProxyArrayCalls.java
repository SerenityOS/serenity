/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package bench.rmi;

import bench.Benchmark;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Benchmark for testing speed of calls with proxy array parameters and return
 * values.
 */
public class ProxyArrayCalls implements Benchmark {

    static class DummyHandler implements InvocationHandler, Serializable {
        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
        {
            return null;
        }
    }

    public static interface DummyInterface {
        public void foo();
    }

    interface Server extends Remote {
        public Proxy[] call(Proxy[] a) throws RemoteException;
    }

    static class ServerImpl extends UnicastRemoteObject implements Server {
        public ServerImpl() throws RemoteException {
        }

        public Proxy[] call(Proxy[] a) throws RemoteException {
            return a;
        }
    }

    static class ServerFactory implements BenchServer.RemoteObjectFactory {
        public Remote create() throws RemoteException {
            return new ServerImpl();
        }
    }

    /**
     * Generate proxy object array of the given size.
     */
    Proxy[] genProxies(int size) throws Exception {
        Class proxyClass =
            Proxy.getProxyClass(DummyInterface.class.getClassLoader(),
                    new Class[] { DummyInterface.class });
        Constructor proxyCons =
            proxyClass.getConstructor(new Class[] { InvocationHandler.class });
        Object[] consArgs = new Object[] { new DummyHandler() };
        Proxy[] proxies = new Proxy[size];

        for (int i = 0; i < size; i++)
            proxies[i] = (Proxy) proxyCons.newInstance(consArgs);

        return proxies;
    }

    /**
     * Issue calls using arrays of proxies as parameters/return values.
     * Arguments: <array size> <# calls>
     */
    public long run(String[] args) throws Exception {
        int size = Integer.parseInt(args[0]);
        int reps = Integer.parseInt(args[1]);
        BenchServer bsrv = Main.getBenchServer();
        Server stub = (Server) bsrv.create(new ServerFactory());
        Proxy[] proxies = genProxies(size);

        long start = System.currentTimeMillis();
        for (int i = 0; i < reps; i++)
            stub.call(proxies);
        long time = System.currentTimeMillis() - start;

        bsrv.unexport(stub, true);
        return time;
    }
}
