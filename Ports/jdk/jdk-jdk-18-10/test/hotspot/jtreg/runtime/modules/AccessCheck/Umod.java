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
 * @summary class p1.c1 defined in m1x tries to access p2.c2 defined in unnamed module.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.module
 * @compile myloaders/MySameClassLoader.java
 * @compile p2/c2.java
 * @compile p1/c1.java
 * @compile p1/c1ReadEdge.java
 * @compile p1/c1Loose.java
 * @run main/othervm -Xbootclasspath/a:. Umod
 */

import static jdk.test.lib.Asserts.*;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import myloaders.MySameClassLoader;

//
// ClassLoader1 --> defines m1x --> packages p1
//                  package p1 in m1x is exported unqualifiedly
//
// class p1.c1 defined in m1x tries to access p2.c2 defined in
// in unnamed module.
//
// Three access attempts occur in this test:
//   1. The first access is not allowed because a strict module
//      cannot read an unnamed module.
//   2. In this scenario a strict module establishes readability
//      to the particular unnamed module it is trying to access.
//      Access is allowed.
//   3. Module m1x in the test_looseModuleLayer() method
//      is transitioned to a loose module, access
//      to all unnamed modules is allowed.
//
public class Umod {

 // Create layers over the boot layer to test different
 // accessing scenarios of a named module to an unnamed module.

 // Module m1x is a strict module and has not established
 // readability to an unnamed module that p2.c2 is defined in.
 public void test_strictModuleLayer() throws Throwable {

     // Define module:     m1x
     // Can read:          java.base
     // Packages:          p1
     // Packages exported: p1 is exported unqualifiedly
     ModuleDescriptor descriptor_m1x =
             ModuleDescriptor.newModule("m1x")
                     .requires("java.base")
                     .exports("p1")
                     .build();

     // Set up a ModuleFinder containing all modules for this layer.
     ModuleFinder finder = ModuleLibrary.of(descriptor_m1x);

     // Resolves "m1x"
     Configuration cf = ModuleLayer.boot()
             .configuration()
             .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

     // map module m1x to class loader.
     // class c2 will be loaded in an unnamed module/loader.
     MySameClassLoader loader = new MySameClassLoader();
     Map<String, ClassLoader> map = new HashMap<>();
     map.put("m1x", loader);

     // Create layer that contains m1x
     ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

     assertTrue(layer.findLoader("m1x") == loader);
     assertTrue(layer.findLoader("java.base") == null);

     // now use the same loader to load class p1.c1
     Class p1_c1_class = loader.loadClass("p1.c1");

     // Attempt access
     try {
         p1_c1_class.newInstance();
         throw new RuntimeException("Test Failed, strict module m1x, type p1.c1, should not be able " +
                                    "to access public type p2.c2 defined in unnamed module");
     } catch (IllegalAccessError e) {
     }
 }

 // Module m1x is a strict module and has established
 // readability to an unnamed module that p2.c2 is defined in.
 public void test_strictModuleUnnamedReadableLayer() throws Throwable {

     // Define module:     m1x
     // Can read:          java.base
     // Packages:          p1
     // Packages exported: p1 is exported unqualifiedly
     ModuleDescriptor descriptor_m1x =
             ModuleDescriptor.newModule("m1x")
                     .requires("java.base")
                     .exports("p1")
                     .build();

     // Set up a ModuleFinder containing all modules for this layer.
     ModuleFinder finder = ModuleLibrary.of(descriptor_m1x);

     // Resolves "m1x"
     Configuration cf = ModuleLayer.boot()
             .configuration()
             .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

     MySameClassLoader loader = new MySameClassLoader();
     // map module m1x to class loader.
     // class c2 will be loaded in an unnamed module/loader.
     Map<String, ClassLoader> map = new HashMap<>();
     map.put("m1x", loader);

     // Create layer that contains m1x
     ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

     assertTrue(layer.findLoader("m1x") == loader);
     assertTrue(layer.findLoader("java.base") == null);

     // now use the same loader to load class p1.c1ReadEdge
     Class p1_c1_class = loader.loadClass("p1.c1ReadEdge");

     try {
       // Read edge between m1x and the unnamed module that loads p2.c2 is established in
       // c1ReadEdge's ctor before attempting access.
       p1_c1_class.newInstance();
     } catch (IllegalAccessError e) {
         throw new RuntimeException("Test Failed, strict module m1x, type p1.c1ReadEdge, should be able to acccess public type " +
                                    "p2.c2 defined in unnamed module: " + e.getMessage());
     }
}

 // Module m1x is a loose module and thus can read all unnamed modules.
 public void test_looseModuleLayer() throws Throwable {

     // Define module:     m1x
     // Can read:          java.base
     // Packages:          p1
     // Packages exported: p1 is exported unqualifiedly
     ModuleDescriptor descriptor_m1x =
             ModuleDescriptor.newModule("m1x")
                     .requires("java.base")
                     .exports("p1")
                     .build();

     // Set up a ModuleFinder containing all modules for this layer.
     ModuleFinder finder = ModuleLibrary.of(descriptor_m1x);

     // Resolves "m1x"
     Configuration cf = ModuleLayer.boot()
             .configuration()
             .resolve(finder, ModuleFinder.of(), Set.of("m1x"));

     MySameClassLoader loader = new MySameClassLoader();
     // map module m1x to class loader.
     // class c2 will be loaded in an unnamed module/loader.
     Map<String, ClassLoader> map = new HashMap<>();
     map.put("m1x", loader);

     // Create layer that contains m1x
     ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

     assertTrue(layer.findLoader("m1x") == loader);
     assertTrue(layer.findLoader("java.base") == null);

     // now use the same loader to load class p1.c1Loose
     Class p1_c1_class = loader.loadClass("p1.c1Loose");

     // change m1x to read all unnamed modules
     Module m1x = layer.findModule("m1x").get();
     jdk.internal.module.Modules.addReadsAllUnnamed(m1x);

     try {
         p1_c1_class.newInstance();
     } catch (IllegalAccessError e) {
         throw new RuntimeException("Test Failed, strict module m1x, type p1.c1Loose, should be able to acccess public type " +
                                    "p2.c2 defined in unnamed module: " + e.getMessage());
     }
 }

 public static void main(String args[]) throws Throwable {
   Umod test = new Umod();
   test.test_strictModuleLayer();                // access denied
   test.test_strictModuleUnnamedReadableLayer(); // access allowed
   test.test_looseModuleLayer();                 // access allowed
 }
}
