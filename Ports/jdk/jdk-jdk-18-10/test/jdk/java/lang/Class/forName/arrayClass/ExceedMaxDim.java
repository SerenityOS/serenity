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

/* @test
 * @bug 7044282
 * @build Class1 Class2 Class3 Class4
 * @run main ExceedMaxDim
 * @summary Make sure you can't get an array class of dimension > 255.
 */

// Class1, Class2, Class3 and Class4 should not have been loaded prior to the
// calls to forName

public class ExceedMaxDim {
                             //0123456789012345678901234567890123456789
    private String brackets = "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
                              "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
                              "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
                              "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
                              "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
                              "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
                              "[[[[[[[[[[[[[[";
    private String name254 = brackets + "Ljava.lang.String;";
    private String name255 = "[" + name254;
    private String name256 = "[" + name255;
    private String name1 = "[Ljava.lang.String;";
    private String bigName;
    private int error = 0;

    private static final ClassLoader IMPLICIT_LOADER = null;

    public ExceedMaxDim() {
        super();

        StringBuilder sb = new StringBuilder(Short.MAX_VALUE + 50);
        for (int i = 0; i < Short.MAX_VALUE + 20; i++)
            sb.append('[');
        sb.append("Ljava.lang.String;");
        bigName = sb.toString();

        if (name256.lastIndexOf('[') != 255) // 256:th [
            throw new RuntimeException("Test broken");
    }

    public static void main(String[] args) throws Exception {
        ExceedMaxDim test = new ExceedMaxDim();
        test.testImplicitLoader();
        test.testOtherLoader();

        if (test.error != 0)
            throw new RuntimeException("Test failed, was able to create array with dim > 255." +
                    " See log for details.");
    }

    private void testImplicitLoader() throws Exception {
        // These four should succeed
        assertSucceedForName(name1, IMPLICIT_LOADER);
        assertSucceedForName(name254, IMPLICIT_LOADER);
        assertSucceedForName(name255, IMPLICIT_LOADER);
        assertSucceedForName(brackets + "[LClass1;", IMPLICIT_LOADER);

        // The following three should fail
        assertFailForName(name256, IMPLICIT_LOADER);
        assertFailForName(bigName, IMPLICIT_LOADER);
        assertFailForName(brackets + "[[LClass2;", IMPLICIT_LOADER);
    }

    private void testOtherLoader() throws Exception {
        ClassLoader cl = ExceedMaxDim.class.getClassLoader();

        // These four should succeed
        assertSucceedForName(name1, cl);
        assertSucceedForName(name254,cl);
        assertSucceedForName(name255, cl);
        assertSucceedForName(brackets + "[LClass3;", cl);

        // The following three should fail
        assertFailForName(name256, cl);
        assertFailForName(bigName, cl);
        assertFailForName(brackets + "[[Class4;", cl);
    }

    private void assertFailForName(String name, ClassLoader cl) {
        Class<?> c;
        try {
            if (cl == null)
                c = Class.forName(name);
            else
                c = Class.forName(name, true, cl);
            error++;
            System.err.println("ERROR: could create " + c);
        } catch (ClassNotFoundException e) {
            ;// ok
        }
    }

    private void assertSucceedForName(String name, ClassLoader cl) {
        Class<?> c;
        try {
            if (cl == null)
                c = Class.forName(name);
            else
                c = Class.forName(name, true, cl);
        } catch (ClassNotFoundException e) {
            error++;
            System.err.println("ERROR: could not create " + name);
        }
    }
}
