/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4099660 4102938
 * @summary Remote classes not extending RemoteObject should be able to
 *          implement hashCode() and equals() methods so that instances
 *          can be successfully compared to RemoteObject instances
 *          (specifically: stubs) that contain the instance's RemoteRef.
 * @author Peter Jones
 *
 * @build NotExtending_Stub NotExtending_Skel
 * @run main/othervm NotExtending
 */


import java.rmi.*;
import java.rmi.server.*;

public class NotExtending implements Remote {

    /** remote stub for this server instance */
    private Remote stub;
    /** value of stub's hash code */
    private int hashValue;
    /** true if the hashValue field has been initialized */
    private boolean hashValueInitialized = false;

    // no declared constructor - rely on implicit no-arg contructor

    public Remote export() throws RemoteException {
        stub = UnicastRemoteObject.exportObject(this);
        setHashValue(stub.hashCode());
        return stub;
    }

    public void unexport() throws RemoteException {
        UnicastRemoteObject.unexportObject(this, true);
    }

    private void setHashValue(int value) {
        hashValue = value;
        hashValueInitialized = true;
    }

    public int hashCode() {
        /*
         * Test fails if the hashCode() method is called (during export)
         * before the correct hash value has been initialized.
         */
        if (!hashValueInitialized) {
            throw new AssertionError(
                "hashCode() invoked before hashValue initialized");
        }
        return hashValue;
    }

    public boolean equals(Object obj) {
        return stub.equals(obj);
    }

    public static void main(String[] args) throws Exception {
        NotExtending server = null;

        try {
            /*
             * Verify that hashCode() is not invoked before it is
             * initialized.  Tests bugid 4102938.
             */
            server = new NotExtending();
            Remote stub = server.export();
            System.err.println("Server exported without invoking hashCode().");

            /*
             * Verify that passing stub to server's equals() method
             * returns true.
             */
            if (server.equals(stub)) {
                System.err.println("server.equals(stub) returns true");
            } else {
                throw new AssertionError("server.equals(stub) returns false");
            }

            /*
             * Verify that passing server to stub's equals() method
             * returns true.  Tests bugid 4099660.
             */
            if (stub.equals(server)) {
                System.err.println("stub.equals(server) returns true");
            } else {
                throw new AssertionError("stub.equals(server) returns false");
            }
        } finally {
            if (server != null) {
                server.unexport();
            }
        }
    }
}
