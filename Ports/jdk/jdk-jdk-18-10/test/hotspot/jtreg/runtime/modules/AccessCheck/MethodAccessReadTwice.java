/*
 Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8174954
 * @summary Test that invokedynamic instructions, that initially throw IAE exceptions
 *          because of a missing module read edge, behave correctly when executed
 *          after the module read edge is added.
 * @compile ModuleLibrary.java
 *          p2/c2.java
 *          p5/c5.java
 *          p7/c7.java
 * @run main/othervm MethodAccessReadTwice
 */

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.ModuleLayer;
import java.lang.Module;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// defines first_mod --> packages p5
// defines second_mod --> package p2, p2 is exported to first_mod
// defines third_mod --> packages p7

public class MethodAccessReadTwice {

    // Create a Layer over the boot layer.
    // Define modules within this layer to test access between
    // publicly defined classes within packages of those modules.
    public void createLayerOnBoot() throws Throwable {

        // Define module:     first_mod
        // Can read:          java.base
        // Packages:          p5
        // Packages exported: p5 is exported unqualifiedly
        ModuleDescriptor descriptor_first_mod =
                ModuleDescriptor.newModule("first_mod")
                        .requires("java.base")
                        .exports("p5")
                        .build();

        // Define module:     second_mod
        // Can read:          java.base
        // Packages:          p2
        // Packages exported: p2 is exported to first_mod
        ModuleDescriptor descriptor_second_mod =
                ModuleDescriptor.newModule("second_mod")
                        .requires("java.base")
                        .exports("p2")
                        .build();

        // Define module:     third_mod
        // Can read:          java.base
        // Packages:          p7
        // Packages exported: p7 is exported unqualifiedly
        ModuleDescriptor descriptor_third_mod =
                ModuleDescriptor.newModule("third_mod")
                        .requires("java.base")
                        .exports("p7")
                        .build();

        // Set up a ModuleFinder containing all modules for this layer
        ModuleFinder finder = ModuleLibrary.of(descriptor_first_mod,
                                               descriptor_second_mod,
                                               descriptor_third_mod);

        // Resolves "first_mod", "second_mod", and "third_mod"
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(),
                         Set.of("first_mod", "second_mod", "third_mod"));

        // Map each module to this class loader
        Map<String, ClassLoader> map = new HashMap<>();
        ClassLoader loader = MethodAccessReadTwice.class.getClassLoader();
        map.put("first_mod", loader);
        map.put("second_mod", loader);
        map.put("third_mod", loader);

        // Create Layer that contains first_mod, second_mod, and third_mod
        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

        Class p2_c2_class = loader.loadClass("p2.c2");
        Class p5_c5_class = loader.loadClass("p5.c5");
        Class p7_c7_class = loader.loadClass("p7.c7");

        Module first_mod = p5_c5_class.getModule();
        Module second_mod = p2_c2_class.getModule();
        Module third_mod = p7_c7_class.getModule();

        p5.c5 c5_obj = new p5.c5();
        p2.c2 c2_obj = new p2.c2();
        p7.c7 c7_obj = new p7.c7();

        // Test that if an invokedynamic instruction gets an IAE exception because
        // of a module read issue, and then the read issue is fixed, that
        // re-executing the same invokedynamic instruction will get the same IAE.

        // First access check for p5.c5 --> call to method5 --> tries to access p2.c2
        try {
            // Should throw IAE because p5.c5's module cannot read p2.c2's module.
            c5_obj.method5(c2_obj);
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

        // Add a read edge from p5/c5's module (first_mod) to p2.c2's module (second_mod)
        c5_obj.methodAddReadEdge(p2_c2_class.getModule());
        // Second access check for p5.c5, should have same result as first
        try {
            c5_obj.method5(c2_obj); // should result in IAE
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


        // Test that if one invokedynamic instruction gets an IAE exception
        // because of a module read issue, and then the read issue is fixed, that
        // a subsequent invokedynamic instruction, that tries the same access,
        // succeeds.
        c7_obj.method7(c2_obj, second_mod); // Should not result in IAE
    }

    public static void main(String args[]) throws Throwable {
      MethodAccessReadTwice test = new MethodAccessReadTwice();
      test.createLayerOnBoot();
    }
}
