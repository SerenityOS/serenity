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
 * @compile/module=java.base java/lang/ModuleHelper.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI AccessCheckWorks
 */

import static jdk.test.lib.Asserts.*;

public class AccessCheckWorks {

    // Check that a class in a package in module_one can successfully access a
    // class in module_two when module_one can read module_two and the class's package
    // has been exported.
    public static void main(String args[]) throws Throwable {
        Object m1x, m2x;

        // Get the java.lang.Module object for module java.base.
        Class jlObject = Class.forName("java.lang.Object");
        Object jlObject_jlM = jlObject.getModule();
        assertNotNull(jlObject_jlM, "jlModule object of java.lang.Object should not be null");

        // Get the class loader for AccessCheckWorks and assume it's also used to
        // load classes p1.c1 and p2.c2.
        ClassLoader this_cldr = AccessCheckWorks.class.getClassLoader();

        // Define a module for p1.
        m1x = ModuleHelper.ModuleObject("module_one", this_cldr, new String[] { "p1" });
        assertNotNull(m1x, "Module should not be null");
        ModuleHelper.DefineModule(m1x, false, "9.0", "m1x/here", new String[] { "p1" });
        ModuleHelper.AddReadsModule(m1x, jlObject_jlM);

        // Define a module for p2.
        m2x = ModuleHelper.ModuleObject("module_two", this_cldr, new String[] { "p2" });
        assertNotNull(m2x, "Module should not be null");
        ModuleHelper.DefineModule(m2x, false, "9.0", "m2x/there", new String[] { "p2" });
        ModuleHelper.AddReadsModule(m2x, jlObject_jlM);

        // Make package p1 in m1x visible to everyone.
        ModuleHelper.AddModuleExportsToAll(m1x, "p1");

        // p1.c1's ctor tries to call a method in p2.c2.  This should work because
        // p1's module can read p2's module and p2 is exported to p1's module.
        ModuleHelper.AddReadsModule(m1x, m2x);
        ModuleHelper.AddModuleExports(m2x, "p2", m1x);
        Class p1_c1_class = Class.forName("p1.c1");
        p1_c1_class.newInstance();
    }
}

