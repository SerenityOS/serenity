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

/*
 * @test
 * @modules java.base/jdk.internal.misc
 * @library /test/lib ..
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @compile/module=java.base java/lang/ModuleHelper.java
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI JVMAddModuleExportToAllUnnamed
 */

import static jdk.test.lib.Asserts.*;

public class JVMAddModuleExportToAllUnnamed {

    // Check that a class in a package in module_one cannot access a class
    // that is in the unnamed module if the accessing package is strict.
    public static void main(String args[]) throws Throwable {
        Object m1x;

        // Get the java.lang.Module object for module java.base.
        Class jlObject = Class.forName("java.lang.Object");
        Object jlObject_jlM = jlObject.getModule();
        assertNotNull(jlObject_jlM, "jlModule object of java.lang.Object should not be null");

        // Get the class loader for JVMAddModuleExportToAllUnnamed and assume it's also used to
        // load class p1.c1.
        ClassLoader this_cldr = JVMAddModuleExportToAllUnnamed.class.getClassLoader();

        // Define a module for p1.
        m1x = ModuleHelper.ModuleObject("module_one", this_cldr, new String[] { "p1" });
        assertNotNull(m1x, "Module should not be null");
        ModuleHelper.DefineModule(m1x, false, "9.0", "m1x/here", new String[] { "p1" });
        ModuleHelper.AddReadsModule(m1x, jlObject_jlM);

        // Make package p1 in m1x visible to everyone.
        ModuleHelper.AddModuleExportsToAll(m1x, "p1");

        // p1.c1's ctor tries to call a method in p2.c2.  This should not work
        // because p2 is in the unnamed module and p1.c1 is strict.
        try {
            Class p1_c1_class = Class.forName("p1.c1");
            p1_c1_class.newInstance();
        } catch (IllegalAccessError e) {
            System.out.println(e.getMessage());
            if (!e.getMessage().contains("does not read unnamed module")) {
                throw new RuntimeException("Wrong message: " + e.getMessage());
            }
        }
    }
}
