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
 * @bug 4472769
 * @summary Stubs and skeletons used to implement the RMI registry
 * implementation and the bootstrap stubs must always follow certain
 * "well known" protocols so that they otherwise need not be in sync--
 * in other words, a registry stub from any arbitrary J2SE vendor and
 * version must be able to communicate with a registry skeleton from
 * any other arbitrary J2SE vendor and version.  In addition to
 * (unfortunately) using the old "-v1.1" stub/skeleton invocation
 * protocol, with its unevolvable operation number scheme, they must
 * always use the same value for the -v1.1 stub/skeleton
 * "interface hash": 4905912898345647071L.
 *
 * @author Peter Jones
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary ReferenceRegistryStub
 * @run main/othervm InterfaceHash
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ObjID;
import java.rmi.server.Operation;
import java.rmi.server.RemoteCall;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import java.util.Arrays;

import sun.rmi.server.UnicastRef;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;

public class InterfaceHash {

    private static final String NAME = "WMM";

    public static void main(String[] args) throws Exception {
        System.err.println("\nRegression test for bug 4472769");

        System.err.println(
            "\n=== verifying that J2SE registry's skeleton uses" +
            "\ncorrect interface hash and operation numbers:");

        Registry testImpl = TestLibrary.createRegistryOnEphemeralPort();
        int regPort = TestLibrary.getRegistryPort(testImpl);
        System.err.println("created test registry on port " + regPort);

        RemoteRef ref = new UnicastRef(
            new LiveRef(new ObjID(ObjID.REGISTRY_ID),
                        new TCPEndpoint("", regPort), false));
        Registry referenceStub = new ReferenceRegistryStub(ref);
        System.err.println("created reference registry stub: " +
                           referenceStub);

        referenceStub.bind(NAME, referenceStub);
        System.err.println("bound name \"" + NAME + "\" in registry");

        String[] list = referenceStub.list();
        System.err.println("list of registry contents: " +
                           Arrays.asList(list));
        if (list.length != 1 || !list[0].equals(NAME)) {
            throw new RuntimeException(
                "TEST FAILED: unexpected list contents");
        }

        Registry result = (Registry) referenceStub.lookup(NAME);
        System.err.println("lookup of name \"" + NAME + "\" returned: " +
                           result);
        if (!result.equals(referenceStub)) {
            throw new RuntimeException(
                "TEST FAILED: unexpected lookup result");
        }

        referenceStub.rebind(NAME, referenceStub);
        referenceStub.unbind(NAME);
        System.err.println("unbound name \"" + NAME + "\"");

        list = referenceStub.list();
        System.err.println("list of registry contents: " +
                           Arrays.asList(list));
        if (list.length != 0) {
            throw new RuntimeException("TEST FAILED: list not empty");
        }

        System.err.println("\n=== verifying that J2SE registry's stub uses" +
                           "correct interface hash:");

        class FakeRemoteRef implements RemoteRef {
            long hash;
            int opnum;
            public RemoteCall newCall(RemoteObject obj, Operation[] op,
                                      int opnum, long hash)
            {
                this.hash = hash;
                this.opnum = opnum;
                throw new UnsupportedOperationException();
            }
            public void invoke(RemoteCall call) { }
            public void done(RemoteCall call) { }
            public Object invoke(Remote obj, Method method,
                                 Object[] args, long hash)
            {
                throw new UnsupportedOperationException();
            }
            public String getRefClass(java.io.ObjectOutput out) {
                return "FakeRemoteRef";
            }
            public int remoteHashCode() { return 1013; }
            public boolean remoteEquals(RemoteRef obj) { return false; }
            public String remoteToString() { return "FakeRemoteRef"; }
            public void writeExternal(java.io.ObjectOutput out) { }
            public void readExternal(java.io.ObjectInput in) { }
        }
        FakeRemoteRef f = new FakeRemoteRef();

        Registry testRegistry = LocateRegistry.getRegistry(regPort);
        System.err.println("created original test registry stub: " +
                           testRegistry);

        Class stubClass = testRegistry.getClass();
        System.err.println("test registry stub class: " + stubClass);

        Constructor cons = stubClass.getConstructor(
            new Class[] { RemoteRef.class });
        Registry testStub = (Registry) cons.newInstance(
            new Object[] { f });
        System.err.println("created new instrumented test registry stub: " +
                           testStub);

        System.err.println("invoking bind:");
        try {
            testStub.bind(NAME, referenceStub);
        } catch (UnsupportedOperationException e) {
        }
        System.err.println("hash == " + f.hash + ", opnum == " + f.opnum);
        if (f.hash != 4905912898345647071L) {
            throw new RuntimeException("TEST FAILED: wrong interface hash");
        } else if (f.opnum != 0) {
            throw new RuntimeException("TEST FAILED: wrong operation number");
        }

        System.err.println("invoking list:");
        try {
            testStub.list();
        } catch (UnsupportedOperationException e) {
        }
        System.err.println("hash == " + f.hash + ", opnum == " + f.opnum);
        if (f.hash != 4905912898345647071L) {
            throw new RuntimeException("TEST FAILED: wrong interface hash");
        } else if (f.opnum != 1) {
            throw new RuntimeException("TEST FAILED: wrong operation number");
        }

        System.err.println("invoking lookup:");
        try {
            testStub.lookup(NAME);
        } catch (UnsupportedOperationException e) {
        }
        System.err.println("hash == " + f.hash + ", opnum == " + f.opnum);
        if (f.hash != 4905912898345647071L) {
            throw new RuntimeException("TEST FAILED: wrong interface hash");
        } else if (f.opnum != 2) {
            throw new RuntimeException("TEST FAILED: wrong operation number");
        }

        System.err.println("invoking rebind:");
        try {
            testStub.rebind(NAME, referenceStub);
        } catch (UnsupportedOperationException e) {
        }
        System.err.println("hash == " + f.hash + ", opnum == " + f.opnum);
        if (f.hash != 4905912898345647071L) {
            throw new RuntimeException("TEST FAILED: wrong interface hash");
        } else if (f.opnum != 3) {
            throw new RuntimeException("TEST FAILED: wrong operation number");
        }

        System.err.println("invoking unbind:");
        try {
            testStub.unbind(NAME);
        } catch (UnsupportedOperationException e) {
        }
        System.err.println("hash == " + f.hash + ", opnum == " + f.opnum);
        if (f.hash != 4905912898345647071L) {
            throw new RuntimeException("TEST FAILED: wrong interface hash");
        } else if (f.opnum != 4) {
            throw new RuntimeException("TEST FAILED: wrong operation number");
        }

        System.err.println("TEST PASSED");
    }
}
