/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that a hidden class has the same module as its lookup class.
 * @library /test/lib
 * @modules jdk.compiler
 * @compile pkg/HasNamedModule.java
 * @run main/othervm HiddenGetModule
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import java.io.IOException;
import java.lang.ModuleLayer;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jdk.test.lib.compiler.InMemoryJavaCompiler;

public class HiddenGetModule {

   static byte unnamedKlassbuf[] = InMemoryJavaCompiler.compile("TestClass",
       "public class TestClass { " +
       "    public static void concat(String one, String two) throws Throwable { " +
       "        System.out.println(one + two);" +
       " } } ");

    public static ModuleFinder finderOf(ModuleDescriptor... descriptors) {

        // Create a ModuleReference for each module
        Map<String, ModuleReference> namesToReference = new HashMap<>();

        for (ModuleDescriptor descriptor : descriptors) {
            String name = descriptor.name();

            URI uri = URI.create("module:/" + name);

            ModuleReference mref = new ModuleReference(descriptor, uri) {
                @Override
                public ModuleReader open() {
                    throw new UnsupportedOperationException();
                }
            };

            namesToReference.put(name, mref);
        }

        return new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                Objects.requireNonNull(name);
                return Optional.ofNullable(namesToReference.get(name));
            }
            @Override
            public Set<ModuleReference> findAll() {
                return new HashSet<>(namesToReference.values());
            }
        };
    }

    public static void main(String[] args) throws Throwable {

        // Test unnamed module.
        Lookup lookup = MethodHandles.lookup();
        Class<?> cl = lookup.defineHiddenClass(unnamedKlassbuf, false, NESTMATE).lookupClass();
        if (cl.getModule() != HiddenGetModule.class.getModule()) {
            throw new RuntimeException("hidden class and lookup class have different unnamed modules");
        }

        // Test named module.
        MyClassLoader myClassLoader = new MyClassLoader();

        // Define a module named HiddenModule containing package pkg.
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("HiddenModule")
                .requires("java.base")
                .exports("pkg")
                .build();

        // Set up a ModuleFinder containing the module for this layer.
        ModuleFinder finder = finderOf(descriptor);

        // Resolves "HiddenModule"
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("HiddenModule"));

        // map module to class loader
        Map<String, ClassLoader> map = new HashMap<>();
        map.put("HiddenModule", myClassLoader);

        // Create layer that contains HiddenModule
        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, map::get);

        byte klassbuf[] = InMemoryJavaCompiler.compile("pkg.TestClass",
            "package pkg; " +
            "public class TestClass { " +
            "    public static void concat(String one, String two) throws Throwable { " +
            "        System.out.println(one + two);" +
            " } } ");

        // Load the class and call the method that defines a hidden class and compares modules.
        Class<?>c = Class.forName("pkg.HasNamedModule", true, myClassLoader);
        if (c.getClassLoader() != myClassLoader) {
            throw new RuntimeException("pkg.HasNamedModule defined by wrong classloader: " + c.getClassLoader());
        }
        Method m = c.getDeclaredMethod("compareModules", byte[].class);
        m.invoke(null, klassbuf);
    }


    public static class MyClassLoader extends ClassLoader {

        public static final String CLASS_NAME = "HasNamedModule";

        static ByteBuffer readClassFile(String name) {
            File f = new File(System.getProperty("test.classes", "."), name);
            try (FileInputStream fin = new FileInputStream(f);
                 FileChannel fc = fin.getChannel()) {
                return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            } catch (IOException e) {
                throw new RuntimeException("Can't open file: " + name + ", " + e.toString());
            }
        }

        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c;
            if (!name.contains(CLASS_NAME)) {
                c = super.loadClass(name, resolve);
            } else {
                // should not delegate to the system class loader
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
            }
            return c;
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!name.contains(CLASS_NAME)) {
                throw new ClassNotFoundException("Unexpected class: " + name);
            }
            return defineClass(name, readClassFile(name.replace(".", File.separator) + ".class"), null);
        }
    } /* MyClassLoader */

}
