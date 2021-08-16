/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that if module m1x can read module m2x, AND package p2 in m2x is
 *          exported qualifiedly to m1x, then class p1.c1 in m1x can read p2.c2 in m2x.
 *          However, p1.c1 tries to access a private method within p2.c2, verify
 *          that the IAE message contains the correct loader and module names.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile p1/c1.jasm
 * @compile p2/c2.jasm
 * @compile myloaders/MySameClassLoader.java
 * @run main/othervm -Xbootclasspath/a:. ExpQualToM1PrivateMethodIAE
 */

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import myloaders.MySameClassLoader;

public class ExpQualToM1PrivateMethodIAE {

    // Create a layer over the boot layer.
    // Define modules within this layer to test access between
    // publically defined classes within packages of those modules.
    public void createLayerOnBoot() throws Throwable {

        // Define module:     m1x
        // Can read:          java.base, m2x
        // Packages:          p1
        // Packages exported: p1 is exported unqualifiedly
        ModuleDescriptor descriptor_m1x =
                ModuleDescriptor.newModule("m1x")
                        .requires("java.base")
                        .requires("m2x")
                        .exports("p1")
                        .build();

        // Define module:     m2x
        // Can read:          java.base
        // Packages:          p2
        // Packages exported: p2 is exported qualifiedly to m1x
        ModuleDescriptor descriptor_m2x =
                ModuleDescriptor.newModule("m2x")
                        .requires("java.base")
                        .exports("p2", Set.of("m1x"))
                        .build();

        // Set up a ModuleFinder containing all modules for this layer.
        ModuleFinder finder = ModuleLibrary.of(descriptor_m1x, descriptor_m2x);

        // Resolves "m1x"
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

        // map each module to the same class loader for this test
        Map<String, ClassLoader> map = new HashMap<>();
        map.put("m1x", MySameClassLoader.loader1);
        map.put("m2x", MySameClassLoader.loader1);

        // Create layer that contains m1x & m2x
        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

        assertTrue(layer.findLoader("m1x") == MySameClassLoader.loader1);
        assertTrue(layer.findLoader("m2x") == MySameClassLoader.loader1);

        // now use the same loader to load class p1.c1
        Class p1_c1_class = MySameClassLoader.loader1.loadClass("p1.c1");
        try {
            p1_c1_class.newInstance();
            throw new RuntimeException("Test Failed, an IAE should be thrown since p2/c2's method2 is private");
        } catch (IllegalAccessError e) {
            String message = e.getMessage();
            System.out.println(e.toString());
            // java.lang.IllegalAccessError:
            //   tried to access private method p2.c2.method2()V from class p1.c1 (p2.c2 is in module m2x of loader
            //   myloaders.MySameClassLoader @<id>; p1.c1 is in module m1x of loader myloaders.MySameClassLoader @<id>)
            if (!message.contains("class p1.c1 tried to access private method 'void p2.c2.method2()' " +
                                  "(p1.c1 is in module m1x of loader myloaders.MySameClassLoader @") ||
                !message.contains("; p2.c2 is in module m2x of loader myloaders.MySameClassLoader @")) {
              throw new RuntimeException("Test Failed, an IAE was thrown with the wrong message: " + e.toString());
            }
        } catch (Throwable e) {
            throw new RuntimeException("Test Failed, an IAE should be thrown since p2/c2's method2 is private");
        }
    }

    public static void main(String args[]) throws Throwable {
      ExpQualToM1PrivateMethodIAE test = new ExpQualToM1PrivateMethodIAE();
      test.createLayerOnBoot();
    }
}
