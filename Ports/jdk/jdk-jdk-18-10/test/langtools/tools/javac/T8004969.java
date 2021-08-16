/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
@test
@bug 8004969
@summary Lambda deserialization

*/

import java.io.*;

public class T8004969 implements Serializable {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    static String mm(String s) { return "mref" + s; }

    String aField = "aF";

    public static void main(String[] args) throws Exception {
        (new T8004969()).doit();
    }

    public void doit() throws Exception {
        String aLocal = "aL";
        int anInt = 99;

        try {
            // Write lambdas out
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);

            write(out, z -> "[" + z + "]" );
            write(out, z -> { String x = z + z; return x + x; } );
            write(out, T8004969::mm );
            write(out, z -> (new LSI() { public String convert(String x) { return "*"+x; }} ).convert(z) );
            write(out, z -> aField + z );
            write(out, z -> aLocal + z );
            write(out, z -> z + anInt );
            out.flush();
            out.close();

            // Read them back
            ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            readAssert(in, "[X]");
            readAssert(in, "XXXX");
            readAssert(in, "mrefX");
            readAssert(in, "*X");
            readAssert(in, "aFX");
            readAssert(in, "aLX");
            readAssert(in, "X99");
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        assertTrue(assertionCount == 7);
    }

    static void write(ObjectOutput out, LSI lamb) throws IOException {
        out.writeObject(lamb);
    }

    static void readAssert(ObjectInputStream in, String expected)  throws IOException, ClassNotFoundException {
        LSI ls = (LSI) in.readObject();
        String result = ls.convert("X");
        System.out.printf("Result: %s\n", result);
        assertTrue(result.equals(expected));
    }
}

interface LSI extends Serializable {
    String convert(String x);
}
