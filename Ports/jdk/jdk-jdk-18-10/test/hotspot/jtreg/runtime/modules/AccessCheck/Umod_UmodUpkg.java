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
 * @summary class p3.c3 defined in an unnamed module tries to access c4 defined in an unnamed package
 *          and an unnamed module.
 *          Access allowed since any class in an unnamed module can read an unnamed module.
 * @library /test/lib
 * @compile myloaders/MySameClassLoader.java
 * @compile c4.java
 * @compile p3/c3.jcod
 * @run main/othervm -Xbootclasspath/a:. Umod_UmodUpkg
 */

import myloaders.MySameClassLoader;

public class Umod_UmodUpkg {

    public void testAccess() throws Throwable {

        Class p3_c3_class = MySameClassLoader.loader1.loadClass("p3.c3");
        try {
            p3_c3_class.newInstance();
        } catch (IllegalAccessError e) {
          System.out.println(e.getMessage());
              throw new RuntimeException("Test Failed, public type c3 defined in an unnamed module should be able " +
                                         "to access public type c4 defined in an unnamed module");
        }
    }

    public static void main(String args[]) throws Throwable {
      Umod_UmodUpkg test = new Umod_UmodUpkg();
      test.testAccess();
    }
}
