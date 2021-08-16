/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @author Gary Ellison
 * @bug 4232694
 * @summary PermissionCollection.setReadOnly() does not preclude using add()
 */

import java.security.*;
import java.net.SocketPermission;
import java.io.FilePermission;
import java.util.PropertyPermission;

public class AddToReadOnlyPermissionCollection {
    public static void main(String args[]) throws Exception {

        try {
            if (args.length == 0) {
                tryAllPC();
                tryBasicPC();
                tryFilePC();
                tryPropPC();
                trySockPC();
            } else {
                for (int i=0; i <args.length; i++) {
                    switch (args[i].toLowerCase().charAt(1)) {
                    case 'a':
                        tryAllPC();
                        break;
                    case 'b':
                        tryBasicPC();
                        break;
                    case 'f':
                        tryFilePC();
                        break;
                    case 'p':
                        tryPropPC();
                        break;
                    case 's':
                        trySockPC();
                        break;
                    default:
                        throw new Exception("usage: AddToReadOnlyPermissonCollection [-a -b -f -p -s]");
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
        System.out.println("Passed. OKAY");
    }

    static void tryPropPC() throws Exception {
        try {
            PropertyPermission p0 = new PropertyPermission("user.home","read");
            PermissionCollection pc = p0.newPermissionCollection();
            pc.setReadOnly();   // this should lock out future adds
            //
            PropertyPermission p1 = new PropertyPermission("java.home","read");
            pc.add(p1);
            throw new
                Exception("Failed...PropertyPermission added to readonly PropertyPermissionCollection.");

        } catch (SecurityException se) {
            System.out.println("PropertyPermissionCollection passed");
        }
    }

    static void trySockPC() throws Exception {
        try {
            SocketPermission p0= new SocketPermission("example.com","connect");
            PermissionCollection pc = p0.newPermissionCollection();
            pc.setReadOnly();   // this should lock out future adds
            //
            SocketPermission p1= new SocketPermission("example.net","connect");
            pc.add(p1);
            throw new
                Exception("Failed...SocketPermission added to readonly SocketPermissionCollection.");

        } catch (SecurityException se) {
            System.out.println("SocketPermissionCollection passed");
        }

    }

    static void tryFilePC() throws Exception {
        try {
            FilePermission p0 = new FilePermission("/tmp/foobar","read");
            PermissionCollection pc = p0.newPermissionCollection();
            pc.setReadOnly();   // this should lock out future adds
            //
            FilePermission p1 = new FilePermission("/tmp/quux","read");
            pc.add(p1);
            throw new
                Exception("Failed...FilePermission added to readonly FilePermissionCollection.");

        } catch (SecurityException se) {
            System.out.println("FilePermissionCollection passed");
        }
    }

    static void tryBasicPC() throws Exception {
        try {
            MyBasicPermission p0 = new MyBasicPermission("BasicPermision");
            PermissionCollection pc = p0.newPermissionCollection();
            pc.setReadOnly();   // this should lock out future adds
            //
            MyBasicPermission p1 = new MyBasicPermission("EvenMoreBasic");
            pc.add(p1);
            throw new
                Exception("Failed...BasicPermission added to readonly BasicPermissionCollection.");

        } catch (SecurityException se) {
            System.out.println("BasicPermissionCollection passed");
        }
    }

    static void tryAllPC() throws Exception {
        try {
            AllPermission p0 = new AllPermission("AllOfIt","read");
            PermissionCollection pc = p0.newPermissionCollection();
            pc.setReadOnly();   // this should lock out future adds
            //
            AllPermission p1 = new AllPermission("SomeOfIt","read");
            pc.add(p1);
            throw new
                Exception("Failed...AllPermission added to readonly AllPermissionCollection.");

        } catch (SecurityException se) {
            System.out.println("AllPermissionCollection passed");
        }
    }

}

class MyBasicPermission extends BasicPermission {
    public MyBasicPermission(String name)  {
        super(name);
    }
}
