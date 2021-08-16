/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI AccessCheckOpen
 */

import java.lang.Module;
import static jdk.test.lib.Asserts.*;

public class AccessCheckOpen {

    // Test that if module1 can read module2 and module2 is open, then
    // p1.c1 can read p2.c2
    public static void main(String args[]) throws Throwable {
        Object m1, m2;

        // Get the java.lang.Module object for module java.base
        Class jlObject = Class.forName("java.lang.Object");
        Object jlObject_jlM = jlObject.getModule();
        assertNotNull(jlObject_jlM, "jlModule object of java.lang.Object should not be null");

        // Get the class loader for AccessCheckOpen, which is also used to
        // load classes p1.c1 and p2.c2
        ClassLoader this_cldr = AccessCheckOpen.class.getClassLoader();

        // Define a module for p1
        m1 = ModuleHelper.ModuleObject("module1", this_cldr, new String[] { "p1" });
        assertNotNull(m1, "Module should not be null");
        ModuleHelper.DefineModule(m1, false, "9.0", "m1/here", new String[] { "p1" });
        ModuleHelper.AddReadsModule(m1, jlObject_jlM);

        // Define a module for p2
        m2 = ModuleHelper.ModuleObject("module2", this_cldr, new String[] { "p2" });
        assertNotNull(m2, "Module should not be null");
        ModuleHelper.DefineModule(m2, true, "9.0", "m2/there", new String[] { "p2" });
        ModuleHelper.AddReadsModule(m2, jlObject_jlM);

        // Make package p1 in m1 visible to everyone so this test can run it
        ModuleHelper.AddModuleExportsToAll(m1, "p1");

        // Let m1 read m2
        ModuleHelper.AddReadsModule(m1, m2);

        // p1.c1's ctor calls a method in p2.c2, and m2 is open.
        // So should not get IllegalAccessError
        Class p1_c1_class = Class.forName("p1.c1");
        p1_c1_class.newInstance();
    }
}

