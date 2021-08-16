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

/**
 * @test
 * @summary Verifies the JVMTI GetAllModules API
 * @requires vm.jvmti
 * @library /test/lib
 * @run main/othervm/native -agentlib:JvmtiGetAllModulesTest JvmtiGetAllModulesTest
 *
 */
import java.lang.module.ModuleReference;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleDescriptor;
import java.lang.module.Configuration;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.function.Supplier;
import java.util.Objects;
import java.util.Optional;
import java.net.URI;
import java.util.HashSet;
import java.util.HashMap;
import java.util.stream.Collectors;
import jdk.test.lib.Asserts;

public class JvmtiGetAllModulesTest {

    static class MyModuleReference extends ModuleReference {
        public MyModuleReference(ModuleDescriptor descriptor, URI uri) {
            super(descriptor, uri);
        }

        // Trivial implementation to make the class non-abstract
        public ModuleReader open() {
            return null;
        }
    }

    private static native Module[] getModulesNative();

    private static Set<Module> getModulesJVMTI() {

        Set<Module> modules = Arrays.stream(getModulesNative()).collect(Collectors.toSet());

        // JVMTI reports unnamed modules, Java API does not
        // remove the unnamed modules here, so the resulting report can be expected
        // to be equal to what Java reports
        modules.removeIf(mod -> !mod.isNamed());

        // jdk.proxy1 and jdk.proxy2 modules are dynamically initialized by Graal code in case Graal VM is used.
        // We need to filter them out because they are not part of boot modules. See more details in JDK-8195156.
        modules.removeIf(mod -> mod.getName().startsWith("jdk.proxy"));

        return modules;
    }

    public static void main(String[] args) throws Exception {

        final String MY_MODULE_NAME = "myModule";

        // Verify that JVMTI reports exactly the same info as Java regarding the named modules
        Asserts.assertEquals(ModuleLayer.boot().modules(), getModulesJVMTI());

        // Load a new named module
        ModuleDescriptor descriptor = ModuleDescriptor.newModule(MY_MODULE_NAME).build();
        ModuleFinder finder = finderOf(descriptor);
        ClassLoader loader = new ClassLoader() {};
        Configuration parent = ModuleLayer.boot().configuration();
        Configuration cf = parent.resolve(finder, ModuleFinder.of(), Set.of(MY_MODULE_NAME));
        ModuleLayer my = ModuleLayer.boot().defineModules(cf, m -> loader);

        // Verify that the loaded module is indeed reported by JVMTI
        Set<Module> jvmtiModules = getModulesJVMTI();
        for (Module mod : my.modules()) {
            if (!jvmtiModules.contains(mod)) {
                throw new RuntimeException("JVMTI did not report the loaded named module: " + mod.getName());
            }
        }

    }

    /**
     * Returns a ModuleFinder that finds modules with the given module
     * descriptors.
     */
    static ModuleFinder finderOf(ModuleDescriptor... descriptors) {

        // Create a ModuleReference for each module
        Map<String, ModuleReference> namesToReference = new HashMap<>();

        for (ModuleDescriptor descriptor : descriptors) {
            String name = descriptor.name();

            URI uri = URI.create("module:/" + name);

            ModuleReference mref = new MyModuleReference(descriptor, uri);

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

}
