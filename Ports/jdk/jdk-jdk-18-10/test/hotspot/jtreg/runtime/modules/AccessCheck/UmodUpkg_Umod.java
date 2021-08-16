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
 * @summary Test public type c5 defined in an unnamed package and unnamed module can
 *          access public type p6.c6 defined in an unnamed module.
 * @library /test/lib
 * @compile myloaders/MySameClassLoader.java
 * @compile p6/c6.java
 * @compile c5.java
 * @run main/othervm -Xbootclasspath/a:. UmodUpkg_Umod
 */

import myloaders.MySameClassLoader;

public class UmodUpkg_Umod {

    public void testAccess() throws Throwable {

        Class c5_class = MySameClassLoader.loader1.loadClass("c5");
        try {
            c5_class.newInstance();
        } catch (IllegalAccessError e) {
          System.out.println(e.getMessage());
              throw new RuntimeException("Test Failed, public type c5 defined in an unnamed package and unnamed "
                      + "module should be able to access public type c6 defined in an unnamed module");
        }
    }

    public static void main(String args[]) throws Throwable {
      UmodUpkg_Umod test = new UmodUpkg_Umod();
      test.testAccess();
    }
}
