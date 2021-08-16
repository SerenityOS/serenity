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
 * @bug 8246778
 * @summary Test that security checks occur for getPermittedSubclasses
 * @library /test/lib
 * @modules java.compiler
 * @build jdk.test.lib.compiler.CompilerUtils jdk.test.lib.compiler.ModuleInfoMaker TestSecurityManagerChecks
 * @run main/othervm -Djava.security.manager=allow --enable-preview TestSecurityManagerChecks named
 * @run main/othervm -Djava.security.manager=allow --enable-preview TestSecurityManagerChecks unnamed
 */

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jdk.test.lib.compiler.*;

public class TestSecurityManagerChecks {

    private static final ClassLoader OBJECT_CL = null;

    public static void main(String[] args) throws Throwable {
        if ("named".equals(args[0])) {
            runNamedModuleTest();
        } else {
            runUnnamedModuleTest();
        }
    }

    private static void runNamedModuleTest() throws Throwable {
        Path classes = compileNamedModuleTest();
        URL[] testClassPath = getTestClassPath();

        //need to use a different ClassLoader to run the test, so that the checks are performed:
        ClassLoader testCL = new URLClassLoader(testClassPath, OBJECT_CL);
        testCL.loadClass("TestSecurityManagerChecks")
              .getDeclaredMethod("doRunNamedModuleTest", Path.class)
              .invoke(null, classes);
    }

    public static void doRunNamedModuleTest(Path classes) throws Throwable {
        Configuration testConfig = ModuleLayer.boot()
                                              .configuration()
                                              .resolve(ModuleFinder.of(),
                                                       ModuleFinder.of(classes),
                                                       List.of("test"));
        ModuleLayer testLayer = ModuleLayer.boot()
                                           .defineModulesWithOneLoader(testConfig,
                                                                         OBJECT_CL);

        // First get hold of the target classes before we enable security
        Class<?> sealed = Class.forName(testLayer.findModule("test").get(), "test.Base");

        //try without a SecurityManager:
        checkPermittedSubclasses(sealed, "test.a.ImplA1",
                                         "test.a.ImplA2",
                                         "test.b.ImplB");

        String[] denyPackageAccess = new String[1];
        int[] checkPackageAccessCallCount = new int[1];

        //try with a SecurityManager:
        SecurityManager sm = new SecurityManager() {
            @Override
            public void checkPackageAccess(String pkg) {
                if (pkg.startsWith("test.")) {
                    checkPackageAccessCallCount[0]++;
                }
                if (Objects.equals(denyPackageAccess[0], pkg)) {
                    throw new SecurityException();
                }
            }
        };

        System.setSecurityManager(sm);

        denyPackageAccess[0] = "test";

        //passes - does not return a class from package "test":
        checkPermittedSubclasses(sealed, "test.a.ImplA1",
                                         "test.a.ImplA2",
                                         "test.b.ImplB");

        if (checkPackageAccessCallCount[0] != 2) {
            throw new AssertionError("Unexpected call count: " +
                                      checkPackageAccessCallCount[0]);
        }

        denyPackageAccess[0] = "test.a";

        try {
            sealed.getPermittedSubclasses();
            throw new Error("getPermittedSubclasses incorrectly succeeded for " +
                             sealed.getName());
        } catch (SecurityException e) {
            System.out.println("OK - getPermittedSubclasses for " + sealed.getName() +
                               " got expected exception: " + e);
        }
    }

    private static Path compileNamedModuleTest() throws IOException {
        Path base = Paths.get(".", "named");
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");

        ModuleInfoMaker maker = new ModuleInfoMaker(src);
        maker.writeJavaFiles("test",
                              "module test {}",
                              "package test; public sealed interface Base permits test.a.ImplA1, test.a.ImplA2, test.b.ImplB, test.c.ImplC {}",
                              "package test.a; public final class ImplA1 implements test.Base {}",
                              "package test.a; public final class ImplA2 implements test.Base {}",
                              "package test.b; public final class ImplB implements test.Base {}",
                              "package test.c; public final class ImplC implements test.Base {}"
                              );

        if (!CompilerUtils.compile(src, classes.resolve("test"), "--enable-preview", "-source", System.getProperty("java.specification.version"))) {
            throw new AssertionError("Compilation didn't succeed!");
        }

        Files.delete(classes.resolve("test").resolve("test").resolve("c").resolve("ImplC.class"));

        return classes;
    }

    private static void runUnnamedModuleTest() throws Throwable {
        Path classes = compileUnnamedModuleTest();
        URL[] testClassPath = getTestClassPath();

        //need to use a different ClassLoader to run the test, so that the checks are performed:
        ClassLoader testCL = new URLClassLoader(testClassPath, OBJECT_CL);
        testCL.loadClass("TestSecurityManagerChecks")
              .getDeclaredMethod("doRunUnnamedModuleTest", Path.class)
              .invoke(null, classes);
    }

    public static void doRunUnnamedModuleTest(Path classes) throws Throwable {
        ClassLoader unnamedModuleCL =
                new URLClassLoader(new URL[] {classes.toUri().toURL()}, OBJECT_CL);

        // First get hold of the target classes before we enable security
        Class<?> sealed = unnamedModuleCL.loadClass("test.Base");

        //try without a SecurityManager:
        checkPermittedSubclasses(sealed, "test.ImplA1",
                                         "test.ImplA2",
                                         "test.ImplB");

        String[] denyPackageAccess = new String[1];
        int[] checkPackageAccessCallCount = new int[1];

        //try with a SecurityManager:
        SecurityManager sm = new SecurityManager() {
            @Override
            public void checkPackageAccess(String pkg) {
                if (pkg.equals("test")) {
                    checkPackageAccessCallCount[0]++;
                }
                if (Objects.equals(denyPackageAccess[0], pkg)) {
                    throw new SecurityException();
                }
            }
        };

        System.setSecurityManager(sm);

        denyPackageAccess[0] = "test.unknown";

        //passes - does not return a class from package "test.unknown":
        checkPermittedSubclasses(sealed, "test.ImplA1",
                                         "test.ImplA2",
                                         "test.ImplB");

        if (checkPackageAccessCallCount[0] != 1) {
            throw new AssertionError("Unexpected call count: " +
                                      checkPackageAccessCallCount[0]);
        }

        denyPackageAccess[0] = "test";

        try {
            sealed.getPermittedSubclasses();
            throw new Error("getPermittedSubclasses incorrectly succeeded for " +
                             sealed.getName());
        } catch (SecurityException e) {
            System.out.println("OK - getPermittedSubclasses for " + sealed.getName() +
                               " got expected exception: " + e);
        }
    }

    private static Path compileUnnamedModuleTest() throws IOException {
        Path base = Paths.get(".", "unnamed");
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");

        ModuleInfoMaker maker = new ModuleInfoMaker(src);
        maker.writeJavaFiles("test",
                              "module test {}",
                              "package test; public sealed interface Base permits ImplA1, ImplA2, ImplB, ImplC {}",
                              "package test; public final class ImplA1 implements test.Base {}",
                              "package test; public final class ImplA2 implements test.Base {}",
                              "package test; public final class ImplB implements test.Base {}",
                              "package test; public final class ImplC implements test.Base {}"
                              );

        Files.delete(src.resolve("test").resolve("module-info.java"));

        if (!CompilerUtils.compile(src.resolve("test"), classes, "--enable-preview", "-source", System.getProperty("java.specification.version"))) {
            throw new AssertionError("Compilation didn't succeed!");
        }

        Files.delete(classes.resolve("test").resolve("ImplC.class"));

        return classes;
    }

    private static void checkPermittedSubclasses(Class<?> c, String... expected) {
        Class<?>[] subclasses = c.getPermittedSubclasses();
        List<String> subclassesNames = Arrays.stream(subclasses)
                                             .map(Class::getName)
                                             .collect(Collectors.toList());
        if (!subclassesNames.equals(Arrays.asList(expected))) {
            throw new AssertionError("Incorrect permitted subclasses: " +
                                       subclassesNames);
        }
    }

    private static URL[] getTestClassPath() {
        return Arrays.stream(System.getProperty("test.class.path")
                                    .split(System.getProperty("path.separator")))
                     .map(TestSecurityManagerChecks::path2URL)
                     .toArray(s -> new URL[s]);
    }

    private static URL path2URL(String p) {
        try {
            return Path.of(p).toUri().toURL();
        } catch (MalformedURLException ex) {
            throw new AssertionError(ex);
        }
    }

}
