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

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class TestLayer {
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Set<String> modules = Set.of("m1", "m2");

    public static void main(String[] args) throws Exception {
        // disable security manager until Class.forName is called.
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            System.setSecurityManager(null);
        }

        ModuleFinder finder = ModuleFinder.of(MODS_DIR);

        Configuration parent = ModuleLayer.boot().configuration();
        Configuration cf = parent.resolveAndBind(ModuleFinder.of(),
                                                 finder,
                                                 modules);

        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = ModuleLayer.boot().defineModulesWithManyLoaders(cf, scl);

        Module m1 = layer.findModule("m1").get();
        Module m2 = layer.findModule("m2").get();

        if (sm != null) {
            System.setSecurityManager(sm);
        }

        // find exported and non-exported class from a named module
        findClass(m1, "p1.A");
        findClass(m1, "p1.internal.B");
        findClass(m2, "p2.C");

        // find class from unnamed module
        ClassLoader ld = TestLayer.class.getClassLoader();
        findClass(ld.getUnnamedModule(), "TestDriver");

        // check if clinit should not be initialized
        // compile without module-path; so use reflection
        Class<?> c = Class.forName(m1, "p1.Initializer");
        Method m = c.getMethod("isInited");
        Boolean isClinited = (Boolean) m.invoke(null);
        if (isClinited.booleanValue()) {
           throw new RuntimeException("clinit should not be invoked");
        }
    }

    static Class<?> findClass(Module module, String cn) {
        Class<?> c = Class.forName(module, cn);
        if (c == null) {
            throw new RuntimeException(cn + " not found in " + module);
        }
        if (c.getModule() != module) {
            throw new RuntimeException(c.getModule() + " != " + module);
        }
        return c;
    }
}
