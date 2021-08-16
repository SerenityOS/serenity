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

/* @test
 * @bug 4186885
 * @clean OriginalClass EvolvedClass
 * @build OriginalClass
 * @run main OriginalClass
 * @build EvolvedClass
 * @run main EvolvedClass
 * @summary To ensure that during deserializing classes, only the highest
 *          non-serializable class in the hierarchy has its no-arg constructor
 *          invoked.
 *
 */

import java.io.*;
public class OriginalClass {

    public static void main(String args[]) throws Exception{
        ASubClass corg = new ASubClass(1);
        ASubClass cnew = null;

        // Serialize the subclass
        FileOutputStream fo = new FileOutputStream("parents.ser");
        try {
            ObjectOutputStream so = new ObjectOutputStream(fo);
            so.writeObject(corg);
            so.flush();
        } finally {
            fo.close();
        }

        System.out.println("Printing the serialized class: ");
        System.out.println();
        System.out.println(corg);
    }
}


class ASubClass implements Serializable {
    private static final long serialVersionUID = 6341246181948372513L;

    int num;

    ASubClass(int num) {
        this.num = num;
    }

    public String toString() {
        return ("\nNum:  " + num);
    }
}
