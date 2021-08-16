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
 * @bug 4028577 4726689
 * @summary Sanity check that Class.getDeclaredClasses() works.
 */
public class Sanity {
    static class Toplevel { }
    class Nested { }

    public static void main(String[] args) throws Exception {
        class BlockLocal { };
        new Object() { };
        Class[] c = Sanity.class.getDeclaredClasses();
        if (c.length < 2)
             throw new Exception("Incorrect number of declared classes");

        for (int i = 0; i < c.length; i++) {
            String name = c[i].getName();
            System.out.println(name);

            if (c[i] != Nested.class && c[i] != Toplevel.class
                && !name.matches("\\D\\w*\\$\\d*"))
                throw new Exception("Unexpected class: " + name);
        }
    }
}
