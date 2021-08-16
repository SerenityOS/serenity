/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.*;

/*
 * @test
 * @modules java.base/jdk.internal.misc
 * @library /test/lib ..
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @build sun.hotspot.WhiteBox
 * @compile/module=java.base java/lang/ModuleHelper.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI AccessCheckAllUnnamed
 */

public class AccessCheckAllUnnamed {

    // Test a series of error conditions for API JVM_AddModuleExportsToAllUnnamed
    // and then test that a class in the unnamed module can access a package in a
    // named module that has been exported to all unnamed modules.
    public static void main(String args[]) throws Throwable {
        Object m1x, m2x;

        // Get the java.lang.Module object for module java.base.
        Class jlObject = Class.forName("java.lang.Object");
        Object jlM = jlObject.getModule();
        assertNotNull(jlM, "jlModule object of java.lang.Object should not be null");

        // Get the class loader for AccessCheckWorks and assume it's also used to
        // load class p2.c2.
        ClassLoader this_cldr = AccessCheckAllUnnamed.class.getClassLoader();

        // Define a module for p3.
        m1x = ModuleHelper.ModuleObject("module_one", this_cldr, new String[] { "p3" });
        assertNotNull(m1x, "Module should not be null");
        ModuleHelper.DefineModule(m1x, false, "9.0", "m1x/there", new String[] { "p3" });
        ModuleHelper.AddReadsModule(m1x, jlM);

        // Define a module for p2.
        m2x = ModuleHelper.ModuleObject("module_two", this_cldr, new String[] { "p2" });
        assertNotNull(m2x, "Module should not be null");
        ModuleHelper.DefineModule(m2x, false, "9.0", "m2x/there", new String[] { "p2" });
        ModuleHelper.AddReadsModule(m2x, jlM);

        try {
            ModuleHelper.AddModuleExportsToAllUnnamed((Module)null, "p2");
            throw new RuntimeException("Failed to get the expected NPE for null module");
        } catch(NullPointerException e) {
            // Expected
        }

        try {
            ModuleHelper.AddModuleExportsToAllUnnamed(m2x, null);
            throw new RuntimeException("Failed to get the expected NPE for null package");
        } catch(NullPointerException e) {
            // Expected
        }

        try {
            ModuleHelper.AddModuleExportsToAllUnnamed(this_cldr, "p2");
            throw new RuntimeException("Failed to get the expected IAE for bad module");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        try {
            ModuleHelper.AddModuleExportsToAllUnnamed(m2x, "p4");
            throw new RuntimeException("Failed to get the expected IAE for package not in module");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Export package p2 in m2x to allUnnamed.
        ModuleHelper.AddModuleExportsToAllUnnamed(m2x, "p2");

        // p1.c1's ctor tries to call a method in p2.c2.  This should succeed because
        // p1 is in an unnamed module and p2.c2 is exported to all unnamed modules.
        Class p1_c1_class = Class.forName("p1.c1");
        try {
            Object c1_obj = p1_c1_class.newInstance();
        } catch (IllegalAccessError f) {
            throw new RuntimeException(
                "Class in unnamed module could not access package p2 exported to all unnamed modules");
        }
    }
}

