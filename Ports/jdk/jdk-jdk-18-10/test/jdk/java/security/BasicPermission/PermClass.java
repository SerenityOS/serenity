/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4511601 7054918
 * @summary BasicPermissionCollection does not set permClass
 *              during deserialization
 */

import java.security.*;
import java.io.*;

public class PermClass {

    public static void main(String[] args) throws Exception {

        String dir = System.getProperty("test.src");
        if (dir == null) {
            dir = ".";
        }

        MyPermission mp = new MyPermission("PermClass");
        if (args != null && args.length == 1 && args[0] != null) {
            // set up serialized file (arg is JDK version)
            PermissionCollection bpc = mp.newPermissionCollection();
            bpc.add(mp);
            File sFile = new File(dir, "PermClass." + args[0]);
            ObjectOutputStream oos = new ObjectOutputStream
                (new FileOutputStream("PermClass." + args[0]));
            oos.writeObject(bpc);
            oos.close();
            System.exit(0);
        }

        // read in a 1.2.1 BasicPermissionCollection
        File sFile = new File(dir, "PermClass.1.2.1");
        try (FileInputStream fis = new FileInputStream(sFile);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            PermissionCollection pc = (PermissionCollection)ois.readObject();
            System.out.println("1.2.1 collection = " + pc);

            if (pc.implies(mp)) {
                System.out.println("JDK 1.2.1 test passed");
            } else {
                throw new Exception("JDK 1.2.1 test failed");
            }
        }

        // read in a 1.3.1 BasicPermissionCollection
        sFile = new File(dir, "PermClass.1.3.1");
        try (FileInputStream fis = new FileInputStream(sFile);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            PermissionCollection pc = (PermissionCollection)ois.readObject();
            System.out.println("1.3.1 collection = " + pc);

            if (pc.implies(mp)) {
                System.out.println("JDK 1.3.1 test passed");
            } else {
                throw new Exception("JDK 1.3.1 test failed");
            }
        }

        // read in a 1.4 BasicPermissionCollection
        sFile = new File(dir, "PermClass.1.4");
        try (FileInputStream fis = new FileInputStream(sFile);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            PermissionCollection pc = (PermissionCollection)ois.readObject();
            System.out.println("1.4 collection = " + pc);

            if (pc.implies(mp)) {
                System.out.println("JDK 1.4 test 1 passed");
            } else {
                throw new Exception("JDK 1.4 test 1 failed");
            }
        }

        // write out current BasicPermissionCollection
        PermissionCollection bpc = mp.newPermissionCollection();
        bpc.add(mp);
        sFile = new File(dir, "PermClass.current");
        try (FileOutputStream fos = new FileOutputStream("PermClass.current");
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(bpc);
        }

        // read in current BasicPermissionCollection
        try (FileInputStream fis = new FileInputStream("PermClass.current");
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            PermissionCollection pc = (PermissionCollection)ois.readObject();
            System.out.println("current collection = " + pc);

            if (pc.implies(mp)) {
                System.out.println("JDK 1.4 test 2 passed");
            } else {
                throw new Exception("JDK 1.4 test 2 failed");
            }
        }
    }
}

class MyPermission extends BasicPermission {
    public MyPermission(String name) {
        super(name);
    }
}
