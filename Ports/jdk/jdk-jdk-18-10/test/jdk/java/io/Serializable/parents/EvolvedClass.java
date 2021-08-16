/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4186885
 */

import java.io.*;

public class EvolvedClass {
    public static void main(String args[]) throws Exception{
        ASubClass corg = new ASubClass(1, "SerializedByEvolvedClass");
        ASubClass cnew = null;

        // Deserialize in to new class object
        FileInputStream fi = new FileInputStream("parents.ser");
        try {
            ObjectInputStream si = new ObjectInputStream(fi);
            cnew = (ASubClass) si.readObject();
        } finally {
            fi.close();
        }

        System.out.println("Printing the deserialized class: ");
        System.out.println();
        System.out.println(cnew);
    }
}


/* During deserialization, the no-arg constructor of a serializable base class
 * must not be invoked.
 */
class ASuperClass implements Serializable {
    private static final long serialVersionUID = 1L;

    String name;

    ASuperClass() {
        /*
         * This method is not to be executed during deserialization for this
         * example. Must call no-arg constructor of class Object which is the
         * base class for ASuperClass.
         */
        throw new Error("ASuperClass: Wrong no-arg constructor invoked");
    }

    ASuperClass(String name) {
        this.name = new String(name);
    }

    public String toString() {
        return("Name:  " + name);
    }
}

class ASubClass extends ASuperClass implements Serializable {
    int num;

    private static final long serialVersionUID =6341246181948372513L;
    ASubClass(int num, String name) {
        super(name);
        this.num = num;
    }

    public String toString() {
        return (super.toString() + "\nNum:  " + num);
    }
}
