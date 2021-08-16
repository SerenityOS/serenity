/*
 * Copyright (c) 1998, 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4185857 4726689
 * @summary The array returned by getDeclaredClasses doesn't have the
 *          array element type tag.
 */
public class TypeTag {

    private static class Inner { }

    public static void main(String[] args) throws Exception {
        Class[] v = null;

        v = Integer.TYPE.getDeclaredClasses();
        if (v == null || v.length != 0)
            throw new Exception("Integer.TYPE.getDeclaredClasses is not working");
        System.out.println("Integer.TYPE: "+ v.toString());

        v = TypeTag.class.getDeclaredClasses();
        if (v == null)
            throw new Exception("TypeTag.class.getDeclaredClasses returned null");
        System.out.println("TypeTag.class: " + v.toString());

        int n = 0;
        for (int i = 0; i < v.length; i++) {
            String name = v[i].getName();
            System.out.print(name);

            if (!name.matches("\\D\\w*\\$\\d*")) {
                n++;
                System.out.println(" -- user class");
            } else {
                System.out.println();
            }
        }

        if (n != 1)
            throw new Exception("TypeTag.class.getDeclaredClasses found too many classes");
    }
}
