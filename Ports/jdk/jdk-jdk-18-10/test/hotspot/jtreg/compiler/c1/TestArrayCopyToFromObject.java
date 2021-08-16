/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160591
 * @summary C1-generated code for System.arraycopy() does not throw an ArrayStoreException if 'dst' is no a "proper" array (i.e., it is java.lang.Object)
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xcomp -XX:-UseCompressedClassPointers -XX:CompileOnly=TestArrayCopyToFromObject.test TestArrayCopyToFromObject
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xcomp -XX:+UseCompressedClassPointers -XX:CompileOnly=TestArrayCopyToFromObject.test TestArrayCopyToFromObject
 */
public class TestArrayCopyToFromObject {

    public void test(Object aArray[]) {
        Object a = new Object();

        try {
            System.arraycopy(aArray, 0, a, 0, 1);
            throw new RuntimeException ("FAILED: Expected ArrayStoreException " +
                                        "(due to destination not being an array) " +
                                        "was not thrown");
        } catch (ArrayStoreException e) {
            System.out.println("PASSED: Expected ArrayStoreException was thrown");
        }

        try {
            System.arraycopy(a, 0, aArray, 0, 1);
            throw new RuntimeException ("FAILED: Expected ArrayStoreException " +
                                        "(due to source not being an array) " +
                                        "was not thrown");
        } catch (ArrayStoreException e) {
            System.out.println("PASSED: Expected ArrayStoreException was thrown");
        }

    }

    public static void main(String args[]) {
        System.out.println("TestArrayCopyToFromObject");
        Object aArray[] = new Object[10];
        for (int i = 0; i < 10; i++) {
            aArray[i] = new Object();
        }
        new TestArrayCopyToFromObject().test(aArray);
    }
}
