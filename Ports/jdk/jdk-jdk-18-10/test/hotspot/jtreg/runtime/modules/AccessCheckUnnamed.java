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
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI AccessCheckUnnamed
 */

public class AccessCheckUnnamed {

    // Test that a class in the unnamed module can not access a package in a
    // named module that has not been unqualifiedly exported.
    public static void main(String args[]) throws Throwable {
        Object m1x, m2x;

        // Get the java.lang.Module object for module java.base.
        Class jlObject = Class.forName("java.lang.Object");
        Object jlObject_jlM = jlObject.getModule();
        assertNotNull(jlObject_jlM, "jlModule object of java.lang.Object should not be null");

        // Get the class loader for AccessCheckWorks and assume it's also used to
        // load class p2.c2.
        ClassLoader this_cldr = AccessCheckUnnamed.class.getClassLoader();

        // Define a module for p2.
        m2x = ModuleHelper.ModuleObject("module_two", this_cldr, new String[] { "p2" });
        assertNotNull(m2x, "Module should not be null");
        ModuleHelper.DefineModule(m2x, false, "9.0", "m2x/there", new String[] { "p2" });
        ModuleHelper.AddReadsModule(m2x, jlObject_jlM);

        // p1.c1's ctor tries to call a method in p2.c2.  This should fail because
        // p1 is in the unnamed module and p2.c2 is not unqualifiedly exported.
        Class p1_c1_class = Class.forName("p1.c1");
        try {
            Object c1_obj = p1_c1_class.newInstance();
            throw new RuntimeException("Failed to get IAE (p2 in m2x is not exported to unnamed module)");
        } catch (IllegalAccessError f) {
            System.out.println(f.getMessage());
            if (!f.getMessage().contains("does not export p2 to unnamed module")) {
                throw new RuntimeException("Wrong message: " + f.getMessage());
            }
        }
    }
}

