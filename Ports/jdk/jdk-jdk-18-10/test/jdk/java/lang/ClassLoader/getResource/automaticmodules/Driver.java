/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @build Driver Main
 *        jdk.test.lib.util.JarUtils
 * @run main Driver
 * @summary Test ClassLoader.getResourceXXX to locate resources in an automatic
 *          module
 */

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

/**
 * The driver creates a JAR file containing p/Foo.class, p/foo.properties,
 * and p/resources/bar.properties. This ensures there are is a resource in
 * a module package and a resource that is not in the module package. The
 * test is then launched to locate every resource in the JAR file.
 */

public class Driver {

    private static final String TEST_CLASSES = System.getProperty("test.classes");

    public static void main(String[] args) throws Exception {
        // create content for JAR file
        Path dir = Files.createTempDirectory("classes");
        Path p = Files.createDirectory(dir.resolve("p"));
        Files.createFile(p.resolve("Foo.class"));
        Files.createFile(p.resolve("foo.properties"));
        Path resources = Files.createDirectory(p.resolve("resources"));
        Files.createFile(resources.resolve("bar.properties"));

        // create the JAR file, including a manifest
        Path jarFile = Paths.get("library-1.0.jar");
        Manifest man = new Manifest();
        Attributes attrs = man.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
        JarUtils.createJarFile(jarFile, man, dir, p);

        // get the module name
        ModuleFinder finder = ModuleFinder.of(jarFile);
        ModuleReference mref = finder.findAll().stream().findAny().orElse(null);
        if (mref == null)
            throw new RuntimeException("Module not found!!!");
        String name = mref.descriptor().name();

        // launch the test with the JAR file on the module path
        if (ProcessTools.executeTestJava("-p", jarFile.toString(),
                                         "--add-modules", name,
                                         "-cp", TEST_CLASSES,
                                         "Main", name)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue() != 0)
            throw new RuntimeException("Test failed - see output");
    }

}
