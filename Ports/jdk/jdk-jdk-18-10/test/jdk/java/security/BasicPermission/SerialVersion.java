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
 * @bug 4502729 7054918
 * @summary BasicPermissionCollection serial version UID incorrect
 */

import java.security.*;
import java.io.*;

public class SerialVersion {

    public static void main(String[] args) throws Exception {
        String dir = System.getProperty("test.src");
        File  sFile =  new File (dir,"SerialVersion.1.2.1");
        // read in a 1.2.1 BasicPermissionCollection
        try (FileInputStream fis = new FileInputStream(sFile);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            PermissionCollection pc = (PermissionCollection)ois.readObject();
            System.out.println("1.2.1 collection = " + pc);
        }

        // read in a 1.3.1 BasicPermissionCollection
        sFile =  new File (dir,"SerialVersion.1.3.1");

        try (FileInputStream fis = new FileInputStream(sFile);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            PermissionCollection pc = (PermissionCollection)ois.readObject();
            System.out.println("1.3.1 collection = " + pc);
        }

        // read in a 1.4 BasicPermissionCollection
        sFile =  new File (dir,"SerialVersion.1.4");
        try (FileInputStream fis = new FileInputStream(sFile);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            PermissionCollection pc = (PermissionCollection)ois.readObject();
            System.out.println("1.4 collection = " + pc);
        }

        // write out current BasicPermissionCollection
        MyPermission mp = new MyPermission("SerialVersionTest");
        PermissionCollection bpc = mp.newPermissionCollection();
        sFile =  new File (dir,"SerialVersion.current");
        try (FileOutputStream fos = new FileOutputStream("SerialVersion.current");
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(bpc);
        }

        // read in current BasicPermissionCollection
        try (FileInputStream fis = new FileInputStream("SerialVersion.current");
                ObjectInputStream ois = new ObjectInputStream(fis)) {
            PermissionCollection pc = (PermissionCollection)ois.readObject();
            System.out.println("current collection = " + pc);
        }
    }
}

class MyPermission extends BasicPermission {
    public MyPermission(String name) {
        super(name);
    }
}
