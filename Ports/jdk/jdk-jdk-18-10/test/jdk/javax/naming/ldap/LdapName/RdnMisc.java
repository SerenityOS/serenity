/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4635618
 * @summary Support for manipulating LDAP Names
 */

import javax.naming.ldap.*;
import javax.naming.InvalidNameException;
import java.util.*;
import javax.naming.directory.*;
import java.io.*;

/**
 * Miscelleneous tests of Rdn methods and serialization test
 */
public class RdnMisc {

    public static void main(String args[])
                throws Exception {
        Attributes attrs = new BasicAttributes();
        attrs.put("a", "Mango");
        attrs.put("l", "Yellow<right");
        attrs.put("b", "Juicy, Fruit");
        attrs.put("k", "Orange>ripe");
        attrs.put("c", "Favourite+Fruit");
        attrs.put("j", "Green#raw");
        attrs.put("d", "Tropical\\Fruit");
        attrs.put("i", "Alfanso;expensive");
        attrs.put("e", "Seasonal\"Fruit");
        attrs.put("h", "Summer");
        attrs.put("f", "Smell=Great");
        attrs.put("g", "Taste==Yummy");

        byte[] mangoJuice = new byte[6];
        for (int i = 0; i < mangoJuice.length; i++) {
            mangoJuice[i] = (byte) i;
        }
        attrs.put("m", mangoJuice);
        Rdn rdn = new Rdn(attrs);

        System.out.println();
        System.out.println("size:" + rdn.size());
        System.out.println("toString():" + rdn.toString());
        System.out.println("getType(): " + rdn.getType());
        System.out.println("getValue(): " + rdn.getValue());

        // test toAttributes
        System.out.println();
        System.out.println("toAttributes(): " + rdn.toAttributes());

        // serialization test
        Rdn rdn2 = new Rdn("cn=Juicy\\, Fruit");
        System.out.println("Serializing rdn:" + rdn2);
        ObjectOutputStream out = new ObjectOutputStream(
                                    new FileOutputStream("rdn.ser"));
        out.writeObject(rdn2);
        out.close();

        ObjectInputStream in = new ObjectInputStream(
                                    new FileInputStream("rdn.ser"));

        System.out.println();
        System.out.println("Deserialized RDN:" + in.readObject());
        in.close();
    }
}
