/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Used with --patch-module to exercise the replacement or addition of classes
 * in modules that are linked into the runtime image.
 */

package jdk.test;

public class Main {

    public static void main(String[] args) throws Exception {

        for (String moduleAndClass : args[0].split(",")) {
            String mn = moduleAndClass.split("/")[0];
            String cn = moduleAndClass.split("/")[1];

            // load class
            Class<?> c = Class.forName(cn);

            // check in expected module
            Module m = c.getModule();
            assertEquals(m.getName(), mn);

            // instantiate object
            Main.class.getModule().addReads(m);
            Object obj = c.newInstance();

            // check that the expected version of the class is loaded
            System.out.print(moduleAndClass);
            String s = obj.toString();
            System.out.println(" says " + s);
            assertEquals(s, "hi");

            // check Module getResourceAsStream
            String rn = cn.replace('.', '/') + ".class";
            assertNotNull(m.getResourceAsStream(rn));
        }
    }


    static void assertEquals(Object o1, Object o2) {
        if (!o1.equals(o2))
            throw new RuntimeException("assertion failed");
    }

    static void assertNotNull(Object o) {
        if (o == null)
            throw new RuntimeException("unexpected null");
    }
}
