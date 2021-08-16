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
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI JVMDefineModule
 */

import static jdk.test.lib.Asserts.*;
import java.sql.Time;

public class JVMDefineModule {

    public static void main(String args[]) throws Throwable {
        MyClassLoader cl = new MyClassLoader();
        Object m;

        // NULL classloader argument, expect success
        m = ModuleHelper.ModuleObject("mymodule", null, new String[] { "mypackage" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "mymodule/here", new String[] { "mypackage" });

/* Invalid test, won't compile.
        // Invalid classloader argument, expect an IAE
        try {
            m = ModuleHelper.ModuleObject("mymodule_one", new Object(), new String[] { "mypackage1" });
            ModuleHelper.DefineModule(m, false, "9.0", "mymodule/here", new String[] { "mypackage1" });
            throw new RuntimeException("Failed to get expected IAE for bad loader");
        } catch(IllegalArgumentException e) {
            // Expected
        }
*/

        // NULL package argument, should not throw an exception
        m = ModuleHelper.ModuleObject("mymoduleTwo", cl, new String[] { "nullpkg" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "mymoduleTwo/here", null);

        // Null module argument, expect an NPE
        try {
            ModuleHelper.DefineModule(null, false, "9.0", "mymodule/here", new String[] { "mypackage1" });
            throw new RuntimeException("Failed to get expected NPE for null module");
        } catch(NullPointerException e) {
            if (!e.getMessage().contains("Null module object")) {
              throw new RuntimeException("Failed to get expected IAE message for null module: " + e.getMessage());
            }
            // Expected
        }

        // Invalid module argument, expect an IAE
        try {
            ModuleHelper.DefineModule(new Object(), false, "9.0", "mymodule/here", new String[] { "mypackage1" });
            throw new RuntimeException("Failed to get expected IAE or NPE for bad module");
        } catch(IllegalArgumentException e) {
            if (!e.getMessage().contains("module is not an instance of type java.lang.Module")) {
              throw new RuntimeException("Failed to get expected IAE message for bad module: " + e.getMessage());
            }
        }

        // NULL module name, expect an IAE or NPE
        try {
            m = ModuleHelper.ModuleObject(null, cl, new String[] { "mypackage2" });
            ModuleHelper.DefineModule(m, false, "9.0", "mymodule/here", new String[] { "mypackage2" });
            throw new RuntimeException("Failed to get expected NPE for NULL module");
        } catch(IllegalArgumentException e) {
            // Expected
        } catch(NullPointerException e) {
            // Expected
        }

        // module name is java.base, expect an IAE
        m = ModuleHelper.ModuleObject("java.base", cl, new String[] { "mypackage3" });
        try {
            ModuleHelper.DefineModule(m, false, "9.0", "mymodule/here", new String[] { "mypackage3" });
            throw new RuntimeException("Failed to get expected IAE for java.base, not defined with boot class loader");
        } catch(IllegalArgumentException e) {
            if (!e.getMessage().contains("Class loader must be the boot class loader")) {
              throw new RuntimeException("Failed to get expected IAE message for java.base: " + e.getMessage());
            }
        }

        // Empty entry in package list, expect an IAE
        m = ModuleHelper.ModuleObject("module.y", cl, new String[] { "mypackageX", "mypackageY" });
        try {
            ModuleHelper.DefineModule(m, false, "9.0", "mymodule/here", new String[] { "mypackageX", "", "mypackageY" });
            throw new RuntimeException("Failed to get IAE for empty package");
        } catch(IllegalArgumentException e) {
            if (!e.getMessage().contains("Invalid package name")) {
              throw new RuntimeException("Failed to get expected IAE message empty package entry: " + e.getMessage());
            }
        }

        // Duplicate module name, expect an ISE
        m = ModuleHelper.ModuleObject("Module_A", cl, new String[] { "mypackage6" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage6" });
        try {
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage6a" });
            throw new RuntimeException("Failed to get ISE for duplicate module");
        } catch(IllegalStateException e) {
            if (!e.getMessage().contains("Module Module_A is already defined")) {
              throw new RuntimeException("Failed to get expected ISE message for duplicate module: " + e.getMessage());
            }
        }

        // Package is already defined for class loader, expect an ISE
        m = ModuleHelper.ModuleObject("dupl.pkg.module", cl, new String[] { "mypackage6b" });
        try {
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage6" });
            throw new RuntimeException("Failed to get ISE for existing package");
        } catch(IllegalStateException e) {
            if (!e.getMessage().contains("Package mypackage6 for module dupl.pkg.module is already in another module, Module_A, defined to the class loader")) {
              throw new RuntimeException("Failed to get expected ISE message for duplicate package: " + e.getMessage());
            }
        }

        // Empty module name, expect an IAE
        try {
            m = ModuleHelper.ModuleObject("", cl, new String[] { "mypackage8" });
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage8" });
            throw new RuntimeException("Failed to get expected IAE for empty module name");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Module name with ';', not allowed in java source
        try {
            m = ModuleHelper.ModuleObject("bad;name", cl, new String[] { "mypackage9" });
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage9" });
            throw new RuntimeException("Failed to get expected IAE for bad;name");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Module name with leading dot, not allowed in java source
        try {
            m = ModuleHelper.ModuleObject(".leadingdot", cl, new String[] { "mypackage9a" });
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage9a" });
            throw new RuntimeException("Failed to get expected IAE for .leadingdot");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Module name with trailing dot, not allowed in java source
        try {
            m = ModuleHelper.ModuleObject("trailingdot.", cl, new String[] { "mypackage9b" });
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage9b" });
            throw new RuntimeException("Failed to get expected IAE for trailingdot.");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // Module name with consecutive dots, not allowed in java source
        try {
            m = ModuleHelper.ModuleObject("trailingdot.", cl, new String[] { "mypackage9b" });
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage9b" });
            throw new RuntimeException("Failed to get expected IAE for trailingdot.");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        // module name with multiple dots, should be okay
        m = ModuleHelper.ModuleObject("more.than.one.dat", cl, new String[] { "mypackage9d" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "mypackage9d" });

        // Zero length package list, should be okay
        m = ModuleHelper.ModuleObject("zero.packages", cl, new String[] { });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { });

        // Package name with dots, should be okay
        m = ModuleHelper.ModuleObject("moduleFive", cl, new String[] { "your.apackage" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "moduleFive", new String[] { "your.apackage" });

        // Invalid package name, expect an IAE
        m = ModuleHelper.ModuleObject("moduleSix", cl, new String[] { "foo" }); // Name irrelevant
        try {
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { ";your/apackage" });
            throw new RuntimeException("Failed to get expected IAE for ;your.apackage");
        } catch(IllegalArgumentException e) {
            if (!e.getMessage().contains("Invalid package name")) {
              throw new RuntimeException("Failed to get expected IAE message for bad package name: " + e.getMessage());
            }
        }

        // Invalid package name, expect an IAE
        m = ModuleHelper.ModuleObject("moduleSeven", cl, new String[] { "foo" }); // Name irrelevant
        try {
            ModuleHelper.DefineModule(m, false, "9.0", "module.name/here", new String[] { "7[743" });
            throw new RuntimeException("Failed to get expected IAE for package 7[743");
        } catch(IllegalArgumentException e) {
            if (!e.getMessage().contains("Invalid package name")) {
              throw new RuntimeException("Failed to get expected IAE message for bad package name: " + e.getMessage());
            }
        }

        // Package named "java" defined to a class loader other than the boot or platform class loader, expect an IAE
        m = ModuleHelper.ModuleObject("modulejavapkgOne", cl, new String[] { "java/foo" });
        try {
            // module m is defined to an instance of MyClassLoader class loader
            ModuleHelper.DefineModule(m, false, "9.0", "modulejavapkgOne", new String[] { "java/foo" });
            throw new RuntimeException("Failed to get expected IAE for package java/foo");
        } catch(IllegalArgumentException e) {
            if (!e.getMessage().contains("prohibited package name")) {
              throw new RuntimeException("Failed to get expected IAE message for prohibited package name: " + e.getMessage());
            }
        }

        // Package named "javabar" defined to a class loader other than the boot or platform class loader, should be ok
        m = ModuleHelper.ModuleObject("modulejavapkgTwo", cl, new String[] { "javabar" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "modulejavapkgTwo", new String[] { "javabar" });

        // Package named "java" defined to the boot class loader, should be ok
        //   m's type is a java.lang.Object, module is java.base
        //   java.base module is defined to the boot loader
        ClassLoader boot_loader = m.getClass().getClassLoader();
        m = ModuleHelper.ModuleObject("modulejavapkgThree", boot_loader, new String[] { "java/foo" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "modulejavapkgThree", new String[] { "java/foo" });

        // Package named "java" defined to the platform class loader, should be ok
        //   java.sql module defined to the platform class loader.
        java.sql.Time jst = new java.sql.Time(45 * 1000);
        ClassLoader platform_loader = jst.getClass().getClassLoader();
        m = ModuleHelper.ModuleObject("modulejavapkgFour", platform_loader, new String[] { "java/foo" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "modulejavapkgFour", new String[] { "java/foo" });

        // module version that is null, should be okay
        m = ModuleHelper.ModuleObject("moduleEight", cl, new String[] { "a_package_8" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, null, "moduleEight/here", new String[] { "a_package_8" });

        // module version that is "", should be okay
        m = ModuleHelper.ModuleObject("moduleNine", cl, new String[] { "a_package_9" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "", "moduleNine/here", new String[] { "a_package_9" });

        // module location that is null, should be okay
        m = ModuleHelper.ModuleObject("moduleTen", cl, new String[] { "a_package_10" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", null, new String[] { "a_package_10" });

        // module location that is "", should be okay
        m = ModuleHelper.ModuleObject("moduleEleven", cl, new String[] { "a_package_11" });
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "", new String[] { "a_package_11" });

        // module with very long package names, should be okay
        String[] longPackageNames = new String[] {
         "mypackage/mypackage/mypackage/mypackage1",
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage2",
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage/" +
         "mypackage/mypackage/mypackage/mypackage3"
        };
        m = ModuleHelper.ModuleObject("moduleTwelve", cl, longPackageNames);
        assertNotNull(m, "Module should not be null");
        ModuleHelper.DefineModule(m, false, "9.0", "moduleTwelve", longPackageNames);
        // Indirectly test that the package names doesn't get truncated when defined
        for (int i = 0; i < longPackageNames.length; i++) {
            ModuleHelper.AddModuleExportsToAllUnnamed(m, longPackageNames[i]);
        }
    }

    static class MyClassLoader extends ClassLoader { }
}
