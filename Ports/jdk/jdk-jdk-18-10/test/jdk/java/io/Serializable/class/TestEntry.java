/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4075221
 * @summary Enable serialize of nonSerializable Class descriptor.
 */

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

class TestEntry {
    public static void main(String args[]) throws Exception {
        File f = new File("tmp.ser");
        if (args[0].compareTo("-s") == 0) {
            FileOutputStream of = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(of);
            Class cl = Class.forName(args[1]);
            oos.writeObject(cl);
            if (ObjectStreamClass.lookup(cl) != null)
                oos.writeObject(cl.newInstance());
            oos.close();
            System.out.println("Serialized Class " + cl.getName());
        } else if (args[0].compareTo("-de") == 0) {
            FileInputStream inf = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(inf);
            Class cl = null;
            try {
                cl = (Class)ois.readObject();
                throw new Error("Expected InvalidClassException to be thrown");
            } catch (InvalidClassException e) {
                System.out.println("Caught expected exception DeSerializing class " + e.getMessage());
            }
            ois.close();
        } else if (args[0].compareTo("-doe") == 0) {
            FileInputStream inf = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(inf);
            Class cl = null;
            cl = (Class)ois.readObject();
            try {
                ois.readObject();
                throw new Error("Expected InvalidClassException to be thrown");
            } catch (InvalidClassException e) {
                System.out.println("Caught expected exception DeSerializing class " + e.getMessage());
            }
            ois.close();
        } else if (args[0].compareTo("-d") == 0) {
            FileInputStream inf = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(inf);
            Class cl = (Class)ois.readObject();
            try {
                ois.readObject();
            } catch (EOFException e) {
            }
            ois.close();
            System.out.println("DeSerialized Class " + cl.getName());
        } else {
            throw new RuntimeException("Unrecognized argument");
        }
    }
}
