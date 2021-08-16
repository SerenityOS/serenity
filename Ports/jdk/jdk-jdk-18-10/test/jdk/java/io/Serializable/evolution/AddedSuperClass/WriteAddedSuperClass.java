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

/* @test
 * @bug 4070080
 *
 * @summary  Test reading an evolved class serialization into the original class
 *           version. Class evolved by adding a superclass.
 *
 * @clean A WriteAddedSuperClass ReadAddedSuperClass ReadAddedSuperClass2
 * @compile WriteAddedSuperClass.java
 * @run main WriteAddedSuperClass
 * @clean A AddedSuperClass
 * @compile ReadAddedSuperClass.java
 * @run main ReadAddedSuperClass
 * @clean A
 * @compile WriteAddedSuperClass.java
 * @run main WriteAddedSuperClass
 * @clean A AddedSuperClass
 * @compile ReadAddedSuperClass2.java
 * @run main ReadAddedSuperClass2
 *
 */

 /*
 *  Part a of test serializes an instance of an evolved class to a serialization stream.
 *  Part b of test deserializes the serialization stream into an instance of
 *  the original class.
 *
 */

import java.io.*;

class AddedSuperClass implements Serializable {
    private static final long serialVersionUID = 1L;

    // Needed at least one field to recreate failure.
    int field;
}

class A extends AddedSuperClass implements Serializable  {
    // Version 1.1 of class A.  Added superclass NewSerializableSuper.
    private static final long serialVersionUID = 1L;
}

public class WriteAddedSuperClass {
    public static void main(String args[]) throws IOException {
        A a = new A();
        File f = new File("tmp.ser");
        ObjectOutput out =
            new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(a);
        out.close();
    }
}
