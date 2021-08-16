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
 * @build sun.hotspot.WhiteBox
 * @compile/module=java.base java/lang/ModuleHelper.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI JVMAddReadsModule
 */

import static jdk.test.lib.Asserts.*;

public class JVMAddReadsModule {

    public static void main(String args[]) throws Throwable {
        MyClassLoader from_cl = new MyClassLoader();
        MyClassLoader to_cl = new MyClassLoader();
        Object from_module, to_module;

        from_module = ModuleHelper.ModuleObject("from_module", from_cl, new String[] { "mypackage" });
        assertNotNull(from_module, "Module should not be null");
        ModuleHelper.DefineModule(from_module, false, "9.0", "from_module/here", new String[] { "mypackage" });

        to_module = ModuleHelper.ModuleObject("to_module", to_cl, new String[] { "yourpackage" });
        assertNotNull(to_module, "Module should not be null");
        ModuleHelper.DefineModule(to_module, false, "9.0", "to_module/here", new String[] { "yourpackage" });

        // Null from_module argument, expect NPE
        try {
            ModuleHelper.AddReadsModule(null, to_module);
            throw new RuntimeException("Failed to get the expected NPE");
        } catch (NullPointerException e) {
            // Expected
        }

        // Null to_module argument, expect NPE
        try {
            ModuleHelper.AddReadsModule(from_module, null);
            throw new RuntimeException("Unexpected NPE was thrown");
        } catch (NullPointerException e) {
            // Expected
        }

        // Null from_module and to_module arguments, expect NPE
        try {
            ModuleHelper.AddReadsModule(null, null);
            throw new RuntimeException("Failed to get the expected NPE");
        } catch (NullPointerException e) {
            // Expected
        }

        // Both modules are the same, should not throw an exception
        ModuleHelper.AddReadsModule(from_module, from_module);

        // Duplicate calls, should not throw an exception
        ModuleHelper.AddReadsModule(from_module, to_module);
        ModuleHelper.AddReadsModule(from_module, to_module);
    }

    static class MyClassLoader extends ClassLoader { }
}
