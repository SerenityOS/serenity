/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4460983
 * @summary This test verifies that when a RemoteObject is being
 * deserialized, if the "external ref type name" is a non-empty
 * string that is not equal to one of the external ref type names
 * required to be supported by the specification, then a
 * ClassNotFoundException is thrown.  Of particular note, with the
 * specification change for 4460983, "ActivatableServerRef" is no
 * longer a supported "external ref type name", and the names of
 * other classes in the internal package sun.rmi.server should
 * produce the same result.
 * @author Peter Jones
 *
 * @run main/othervm UnrecognizedRefType
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.server.Operation;
import java.rmi.server.RemoteCall;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;

public class UnrecognizedRefType {
    public static void main(String[] args) throws Exception {
        System.err.println("\nRegression test for bug 4460983\n");

        test(new FakeRemoteObject("ActivatableServerRef"));
        test(new FakeRemoteObject("MarshalInputStream"));
        test(new FakeRemoteObject("XXX"));

        System.err.println("TEST PASSED");
    }

    private static void test(RemoteObject obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(obj);
        ByteArrayInputStream bin =
            new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        try {
            Object obj2 = in.readObject();
            System.err.println(
                "Object unexpectedly deserialized successfully: " + obj2);
            throw new RuntimeException(
                "TEST FAILED: object successfully deserialized");
        } catch (ClassNotFoundException e) {
            System.err.println("ClassNotFoundException as expected:");
            e.printStackTrace();
        } // other exceptions cause test failure
    }

    private static class FakeRemoteObject extends RemoteObject {
        FakeRemoteObject(String refType) {
            super(new FakeRemoteRef(refType));
        }
    }

    private static class FakeRemoteRef implements RemoteRef {
        private final String refType;

        FakeRemoteRef(String refType) {
            this.refType = refType;
        }

        public Object invoke(Remote obj,
                             Method method,
                             Object[] params,
                             long opnum)
        {
            throw new UnsupportedOperationException();
        }

        public RemoteCall newCall(RemoteObject obj,
                                  Operation[] op,
                                  int opnum,
                                  long hash)
        {
            throw new UnsupportedOperationException();
        }

        public void invoke(RemoteCall call) {
            throw new UnsupportedOperationException();
        }

        public void done(RemoteCall call) {
            throw new UnsupportedOperationException();
        }

        public String getRefClass(java.io.ObjectOutput out) {
            return refType;
        }

        public int remoteHashCode() { return hashCode(); }
        public boolean remoteEquals(RemoteRef obj) { return equals(obj); }
        public String remoteToString() { return toString(); }

        public void readExternal(ObjectInput in) {
            throw new UnsupportedOperationException();
        }

        public void writeExternal(ObjectOutput out) {
            // no data to write
        }
    }
}
