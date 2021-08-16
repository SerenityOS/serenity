/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.compiler
 * @summary Test java.lang.Module methods that specify permission checks
 * @run main/othervm -Djava.security.manager=allow -Djava.security.policy=${test.src}/allow.policy WithSecurityManager allow
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager deny
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Test java.lang.Module methods that specify permission checks.
 */

public class WithSecurityManager {

    // a module that will be loaded into a child layer
    static final String ANOTHER_MODULE          = "jdk.compiler";
    static final String ANOTHER_MODULE_RESOURCE = "com/sun/tools/javac/Main.class";

    public static void main(String[] args) throws IOException {
        boolean allow = args[0].equals("allow");

        // base module, in the boot layer
        Module base = Object.class.getModule();

        // another module, in a child layer
        Module other = loadModuleInChildLayer(ANOTHER_MODULE);
        assertTrue(other.getLayer() != ModuleLayer.boot());

        System.setSecurityManager(new SecurityManager());

        test(base, "java/lang/Object.class", allow);
        test(other, ANOTHER_MODULE_RESOURCE, allow);
    }

    /**
     * Test the permission checks by invoking methods on the given module.
     *
     * If {@code allow} is {@code true} then the permission checks should succeed.
     */
    static void test(Module m, String name, boolean allow) throws IOException {

        // test Module::getClassLoader
        System.out.format("Test getClassLoader on %s ...%n", m);
        try {
            ClassLoader cl = m.getClassLoader();
            System.out.println(cl);
            if (!allow)
                assertTrue("getClassLoader should have failed", false);
        } catch (SecurityException e) {
            System.out.println(e + " thrown");
            if (allow)
                throw e;
        }

        // test Module::getResourceAsStream
        System.out.format("Test getResourceAsStream(\"%s\") on %s ...%n", name, m);
        try (InputStream in = m.getResourceAsStream(name)) {
            System.out.println(in);
            if (allow && (in == null))
                assertTrue(name + " not found", false);
            if (!allow && (in != null))
                assertTrue(name + " should not be found", false);
        }

    }

    /**
     * Create a module layer that contains the given system module.
     */
    static Module loadModuleInChildLayer(String mn) {
        Optional<ModuleReference> omref = ModuleFinder.ofSystem().find(mn);
        assertTrue("module " + mn + " not a system module", omref.isPresent());

        // create a ModuleFinder that only finds this module
        ModuleReference mref = omref.get();
        ModuleFinder finder = new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                if (name.equals(mn))
                    return Optional.of(mref);
                else
                    return Optional.empty();
            }

            @Override
            public Set<ModuleReference> findAll() {
                return Collections.singleton(mref);
            }
        };

        // create a child configuration and layer with this module
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration cf = bootLayer
            .configuration()
            .resolve(finder, ModuleFinder.of(), Set.of(ANOTHER_MODULE));
        ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, null);

        Optional<Module> om = layer.findModule(mn);
        assertTrue("module " + mn + " not in child layer", om.isPresent());
        return om.get();
    }

    static void assertTrue(String msg, boolean e) {
        if (!e)
            throw new RuntimeException(msg);
    }

    static void assertTrue(boolean e) {
        if (!e)
            throw new RuntimeException();
    }

}
