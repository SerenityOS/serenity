/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package test;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    private static final Path MODS_DIR = Paths.get(System.getProperty("jdk.module.path"));
    static final String MODULE_NAME = "jdk.translet";

    public static void main(String[] args) throws Exception {

        ModuleFinder finder = ModuleFinder.of(MODS_DIR);
        ModuleLayer layerBoot = ModuleLayer.boot();

        Configuration cf = layerBoot
                .configuration()
                .resolve(ModuleFinder.of(), finder, Set.of(MODULE_NAME));

        Module testModule = Main.class.getModule();
        ClassLoader scl = ClassLoader.getSystemClassLoader();

        // Create an unique module/class loader in a layer above the boot layer.
        // Export this module to the jdk.test/test package.
        Callable<Void> task = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ModuleLayer layer = ModuleLayer.boot().defineModulesWithOneLoader(cf, scl);
                Module transletModule = layer.findModule(MODULE_NAME).get();
                testModule.addExports("test", transletModule);
                Class<?> c = layer.findLoader(MODULE_NAME).loadClass("translet.Main");
                Method method = c.getDeclaredMethod("go");
                method.invoke(null);
                return null;
            }
        };

        List<Future<Void>> results = new ArrayList<>();

        // Repeatedly create the layer above stressing the exportation of
        // package jdk.test/test to several different modules.
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(100, Runtime.getRuntime().availableProcessors()*10));
        try {
            for (int i = 0; i < 10000; i++) {
                results.add(pool.submit(task));
            }
        } finally {
            pool.shutdown();
        }

        int passed = 0;
        int failed = 0;

        // The failed state should be 0, the created modules in layers above the
        // boot layer should be allowed access to the contents of the jdk.test/test
        // package since that package was exported to the transletModule above.
        for (Future<Void> result : results) {
            try {
                result.get();
                passed++;
            } catch (Throwable x) {
                x.printStackTrace();
                failed++;
            }
        }

        System.out.println("passed: " + passed);
        System.out.println("failed: " + failed);
    }

    public static void callback() { }
}
