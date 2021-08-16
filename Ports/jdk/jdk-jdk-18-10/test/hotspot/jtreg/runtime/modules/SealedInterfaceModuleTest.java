/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8225056 8246337
 * @modules java.base/jdk.internal.misc
 * @library /test/lib ..
 * @compile sealedP1/SuperInterface.jcod
 * @compile sealedP1/C1.java sealedP2/C2.java sealedP3/C3.java
 * @build sun.hotspot.WhiteBox
 * @compile/module=java.base java/lang/ModuleHelper.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI SealedInterfaceModuleTest
 */

public class SealedInterfaceModuleTest {

    // Test sub-classing a sealed super interface in a named module. In this test,
    // sealed interface sealedP1/SuperInterface permits sealedP1.C1, sealedP2.C2, and
    // sealedP3.C3.  All three of those classes implement sealedP1/SuperInterface.

    public static void main(String args[]) throws Throwable {
        Object m1x, m2x;

        // Get the class loader for SealedInterfaceModuleTest and assume it's also used
        // to load the other classes.
        ClassLoader this_cldr = AccessCheckRead.class.getClassLoader();

        // Define a module for packages sealedP1 and sealedP2.
        m1x = ModuleHelper.ModuleObject("module_one", this_cldr,
                                        new String[] { "sealedP1", "sealedP2" });
        ModuleHelper.DefineModule(m1x, false, "9.0", "m1x/here",
                                  new String[] { "sealedP1", "sealedP2" });

        // Define a module for package sealedP3 with the same class loader.
        m2x = ModuleHelper.ModuleObject("module_two", this_cldr, new String[] { "sealedP3" });
        ModuleHelper.DefineModule(m2x, false, "9.0", "m2x/there", new String[] { "sealedP3" });

        // Make package sealedP1 in m1x visible to everyone because it contains
        // the sealed super interface for C1, C2, and C3.
        ModuleHelper.AddModuleExportsToAll(m1x, "sealedP1");
        ModuleHelper.AddReadsModule(m2x, m1x);

        // Test subtype in the same named package and named module as its super
        // interface.  This should succeed.
        // Class sealedP1.C1 implements interface sealedP1.SuperInterface.
        Class p1_C1_class = Class.forName("sealedP1.C1");

        // Test non-public class in same module but different package than its
        // super interface. This should throw ICCE.
        // Class sealedP2.C2 implements interface class sealedP1.SuperInterface.
        try {
            Class p2_C2_class = Class.forName("sealedP2.C2");
            throw new RuntimeException("Expected IncompatibleClassChangeError exception not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("cannot implement sealed interface")) {
                throw new RuntimeException("Wrong IncompatibleClassChangeError exception thrown: " + e.getMessage());
            }
        }

        // Test subtype in a different module than its super type.  This should
        // fail even though they have the same class loader.
        // Class sealedP3.C3 implements interface sealedP1.SuperInterface.
        try {
            Class p3_C3_class = Class.forName("sealedP3.C3");
            throw new RuntimeException("Expected IncompatibleClassChangeError exception not thrown");
        } catch (IncompatibleClassChangeError e) {
            if (!e.getMessage().contains("cannot implement sealed interface")) {
                throw new RuntimeException("Wrong IncompatibleClassChangeError exception thrown: " + e.getMessage());
            }
        }

    }
}
