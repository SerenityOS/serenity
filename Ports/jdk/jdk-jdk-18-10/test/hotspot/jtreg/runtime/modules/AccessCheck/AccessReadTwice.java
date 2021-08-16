/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Class p1.c1 in module first_mod cannot read p2.c2 in module second_mod,
 *          even after a read edge is added between first_mod and second_mod.
 *          Ensures constant access check answers when not accessible due to readability.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.module
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @compile p4/c4.java
 * @run main/othervm AccessReadTwice
 */

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//
// ClassLoader1 --> defines first_mod --> packages p1, p4
//                  defines second_mod --> package p2
//
// package p2 in second_mod is exported to first_mod
//
// class p1.c1 defined in first_mod tries to access p2.c2 defined in second_mod
// Access is not allowed, even after a read edge is added between first_mod and second_mod.

public class AccessReadTwice {

    // Create a layer over the boot layer.
    // Define modules within this layer to test access between
    // publicly defined classes within packages of those modules.
    public void createLayerOnBoot() throws Throwable {

        // Define module:     first_mod
        // Can read:          java.base
        // Packages:          p1, p4
        // Packages exported: none
        ModuleDescriptor descriptor_first_mod =
                ModuleDescriptor.newModule("first_mod")
                        .requires("java.base")
                        .packages(Set.of("p1", "p4"))
                        .build();

        // Define module:     second_mod
        // Can read:          java.base
        // Packages:          p2
        // Packages exported: p2 is exported to first_mod
        ModuleDescriptor descriptor_second_mod =
                ModuleDescriptor.newModule("second_mod")
                        .requires("java.base")
                        .exports("p2", Set.of("first_mod"))
                        .build();

        // Set up a ModuleFinder containing all modules for this layer
        ModuleFinder finder = ModuleLibrary.of(descriptor_first_mod, descriptor_second_mod);

        // Resolves "first_mod" and "second_mod"
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("first_mod", "second_mod"));

        // Map each module to this class loader
        Map<String, ClassLoader> map = new HashMap<>();
        ClassLoader loader = AccessReadTwice.class.getClassLoader();
        map.put("first_mod", loader);
        map.put("second_mod", loader);

        // Create layer that contains first_mod & second_mod
        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

        assertTrue(layer.findLoader("first_mod") == loader);
        assertTrue(layer.findLoader("second_mod") == loader);
        assertTrue(layer.findLoader("java.base") == null);

        Class p2_c2_class = loader.loadClass("p2.c2");
        Class p1_c1_class = loader.loadClass("p1.c1");
        Class p4_c4_class = loader.loadClass("p4.c4");

        Module first_mod = p1_c1_class.getModule();
        Module second_mod = p2_c2_class.getModule();

        // Export first_mod/p1 and first_mod/p4 to all unnamed modules so that
        // this test can use them
        jdk.internal.module.Modules.addExportsToAllUnnamed(first_mod, "p1");
        jdk.internal.module.Modules.addExportsToAllUnnamed(first_mod, "p4");

        // First access check for p1.c1
        try {
            p1_c1_class.newInstance();
            throw new RuntimeException("Test Failed, module first_mod should not have access to p2.c2");
        } catch (IllegalAccessError e) {
            String message = e.getMessage();
            if (!(message.contains("cannot access") &&
                  message.contains("because module first_mod does not read module second_mod"))) {
                throw new RuntimeException("Wrong message: " + message);
            } else {
                System.out.println("Test Succeeded at attempt #1");
            }
        }

        // Add a read edge from p4/c4's module (first_mod) to second_mod
        p4.c4 c4_obj = new p4.c4();
        c4_obj.addReads(second_mod);

        // Second access check for p1.c1, should have same result as first
        try {
            p1_c1_class.newInstance();
            throw new RuntimeException("Test Failed, access should have been cached above");
        } catch (IllegalAccessError e) {
            String message = e.getMessage();
            if (!(message.contains("cannot access") &&
                  message.contains("because module first_mod does not read module second_mod"))) {
                throw new RuntimeException("Wrong message: " + message);
            } else {
                System.out.println("Test Succeeded at attempt #2");
            }
        }
    }

    public static void main(String args[]) throws Throwable {
      AccessReadTwice test = new AccessReadTwice();
      test.createLayerOnBoot();
    }
}
