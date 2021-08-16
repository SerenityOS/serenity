/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary class p1.c1 defined in an unnamed module tries to access p2.c2
 *          defined in an unnamed module. Access allowed since unnamed module
 *          can read unnamed module even when class p1.c1 is loaded by
 *          a different loader than p2.c2.
 * @library /test/lib
 * @compile myloaders/MyDiffClassLoader.java
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @run main/othervm -Xbootclasspath/a:. UmodDiffCL_Umod
 */

import myloaders.MyDiffClassLoader;

// class p1.c1 defined in an unnamed module tries to access p2.c2 defined in
// in an unnamed module.
// Access allowed since unnamed module can read unnamed module even when
//                class p1.c1 is loaded by a different loader than p2.c2
//                and all packages in an unnamed module are exported unqualifiedly.
public class UmodDiffCL_Umod {

    public static void main(String args[]) throws Throwable {
        Class p1_c1_class = MyDiffClassLoader.loader1.loadClass("p1.c1");
        try {
            p1_c1_class.newInstance();
        } catch (IllegalAccessError e) {
            throw new RuntimeException("Test Failed, unnamed module can access unnamed module");
        }
    }
}
