/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4070080
 *
 * @build WriteAddedSuperClass ReadAddedSuperClass2
 * @run main ReadAddedSuperClass2
 * @summary  2nd part of test deserializes the serialization stream into an
 *           instance of the original class WHEN the AddedSuperClass exists
 *           locally.
 *
 * Description of failure:
 *
 * If you delete AddedSuperClass.class before running this test,
 * both JDK 1.1.x and 1.2 result in ClassNotFoundException for class
 * AddedSuperClass.  If the .class file is not deleted, 1.2 does not
 * fail. However, JDK 1.1.4 core dumps dereferencing a null class handle
 * in the native method for inputClassDescriptor.
 */

import java.io.*;

class AddedSuperClass implements Serializable {
    private static final long serialVersionUID = 1L;

    // Needed at least one field to recreate failure.
    int field;
}

class A implements Serializable {
    // Version 1.0 of class A.
    private static final long serialVersionUID = 1L;
}

public class ReadAddedSuperClass2 {
    public static void main(String args[]) throws IOException, ClassNotFoundException {
        File f = new File("tmp.ser");
        ObjectInput in =
            new ObjectInputStream(new FileInputStream(f));
        A a = (A)in.readObject();
        in.close();
    }
}
