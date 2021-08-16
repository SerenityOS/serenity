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

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//
// ClassLoader1 --> defines m1x --> packages p1
// ClassLoader2 --> defines m2x --> packages p2
// Java System Class Loader --> defines m3x --> packages p3
//
// m1x can read m2x
// package p2 in m2x is exported to m1x and m3x
//
// class p1.c1 defined in m1x tries to access p2.c2 defined in m2x
// Access allowed since m1x can read m2x and package p2 is exported to m1x.
//
public class ModuleNonBuiltinCLMain {

    // Create a layer over the boot layer.
    // Define modules within this layer to test access between
    // publically defined classes within packages of those modules.
    public void createLayerOnBoot() throws Throwable {

        // Define module:     m1x
        // Can read:          java.base, m2x
        // Packages:          p1
        // Packages exported: p1 is exported to unqualifiedly
        ModuleDescriptor descriptor_m1x =
                ModuleDescriptor.newModule("m1x")
                        .requires("java.base")
                        .requires("m2x")
                        .exports("p1")
                        .build();

        // Define module:     m2x
        // Can read:          java.base, m3x
        // Packages:          p2
        // Packages exported: package p2 is exported to m1x and m3x
        Set<String> targets = new HashSet<>();
        targets.add("m1x");
        targets.add("m3x");
        ModuleDescriptor descriptor_m2x =
                ModuleDescriptor.newModule("m2x")
                        .requires("java.base")
                        .requires("m3x")
                        .exports("p2", targets)
                        .build();

        // Define module:     m3x
        // Can read:          java.base
        // Packages:          p3
        // Packages exported: none
        ModuleDescriptor descriptor_m3x =
                ModuleDescriptor.newModule("m3x")
                        .requires("java.base")
                        .build();

        // Set up a ModuleFinder containing all modules for this layer.
        ModuleFinder finder = ModuleLibrary.of(descriptor_m1x, descriptor_m2x, descriptor_m3x);

        // Resolves "m1x"
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

        // map each module to differing user defined class loaders for this test
        Map<String, ClassLoader> map = new HashMap<>();
        Loader1 cl1 = new Loader1();
        Loader2 cl2 = new Loader2();
        ClassLoader cl3 = ClassLoader.getSystemClassLoader();
        map.put("m1x", cl1);
        map.put("m2x", cl2);
        map.put("m3x", cl3);

        // Create layer that contains m1x & m2x
        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);
        assertTrue(layer.findLoader("m1x") == cl1);
        assertTrue(layer.findLoader("m2x") == cl2);
        assertTrue(layer.findLoader("m3x") == cl3);
        assertTrue(layer.findLoader("java.base") == null);

        // now use the same loader to load class p1.c1
        Class p1_c1_class = cl1.loadClass("p1.c1");
        try {
            p1_c1_class.newInstance();
        } catch (IllegalAccessError e) {
            throw new RuntimeException("Test Failed, an IAE should not be thrown since p2 is exported qualifiedly to m1x");
        }
    }

    public static void main(String args[]) throws Throwable {
      ModuleNonBuiltinCLMain test = new ModuleNonBuiltinCLMain();
      test.createLayerOnBoot();
    }

    static class Loader1 extends ClassLoader { }
    static class Loader2 extends ClassLoader { }
}
