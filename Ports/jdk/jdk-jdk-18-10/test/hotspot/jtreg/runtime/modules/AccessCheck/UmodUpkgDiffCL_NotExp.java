/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary class c5 in an unnamed module can read module m2x, but package p6 in module m2x is not exported.
 *          Access denied since even though unnamed module can read all modules, p6 in module m2x is not exported at all.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile myloaders/MyDiffClassLoader.java
 * @compile p6/c6.java
 * @compile c5.java
 * @run main/othervm -Xbootclasspath/a:. UmodUpkgDiffCL_NotExp
 */

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import myloaders.MyDiffClassLoader;

//
// ClassLoader1 --> defines m1x --> no packages
// ClassLoader2 --> defines m2x --> packages p6
//
// m1x can read m2x
// package p6 in m2x is not exported
//
// class c5 defined in unnamed module tries to access p6.c6 defined in m2x
// Access denied since even though unnamed module can read all modules, p6
// in module m2x is not exported at all.
//
public class UmodUpkgDiffCL_NotExp {

    // Create a layer over the boot layer.
    // Define modules within this layer to test access between
    // publically defined classes within packages of those modules.
    public void createLayerOnBoot() throws Throwable {

        // Define module:     m1x
        // Can read:          java.base, m2x
        // Packages:          none
        // Packages exported: none
        ModuleDescriptor descriptor_m1x =
                ModuleDescriptor.newModule("m1x")
                        .requires("java.base")
                        .requires("m2x")
                        .build();

        // Define module:     m2x
        // Can read:          java.base
        // Packages:          p6
        // Packages exported: none
        ModuleDescriptor descriptor_m2x =
                ModuleDescriptor.newModule("m2x")
                        .requires("java.base")
                        .packages(Set.of("p6"))
                        .build();

        // Set up a ModuleFinder containing all modules for this layer.
        ModuleFinder finder = ModuleLibrary.of(descriptor_m1x, descriptor_m2x);

        // Resolves "m1x"
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

        // map each module to differing class loaders for this test
        Map<String, ClassLoader> map = new HashMap<>();
        map.put("m1x", MyDiffClassLoader.loader1);
        map.put("m2x", MyDiffClassLoader.loader2);

        // Create layer that contains m1x & m2x
        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

        assertTrue(layer.findLoader("m1x") == MyDiffClassLoader.loader1);
        assertTrue(layer.findLoader("m2x") == MyDiffClassLoader.loader2);
        assertTrue(layer.findLoader("java.base") == null);

        // now use the same loader to load class c5
        // NOTE: module m1x does not define any packages.
        //       c5 will be loaded in an unnamed module.
        Class c5_class = MyDiffClassLoader.loader1.loadClass("c5");
        try {
            c5_class.newInstance();
            throw new RuntimeException("Failed to get IAE (p6 in m2x is not exported to " +
                                       "an unnamed module that c5 is defined within)");
        } catch (IllegalAccessError e) {
          System.out.println(e.getMessage());
          if (!e.getMessage().contains("does not export")) {
              throw new RuntimeException("Wrong message: " + e.getMessage());
          }
        }
    }

    public static void main(String args[]) throws Throwable {
      UmodUpkgDiffCL_NotExp test = new UmodUpkgDiffCL_NotExp();
      test.createLayerOnBoot();
    }
}
