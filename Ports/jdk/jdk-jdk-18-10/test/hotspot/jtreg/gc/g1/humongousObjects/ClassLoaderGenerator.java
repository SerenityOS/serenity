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

package gc.g1.humongousObjects;

import gc.testlibrary.Helpers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates non-humongous and humongous class loaders.
 * Since the generation depends on current host architecture it cannot be done as part of pre-compilation step
 */
public class ClassLoaderGenerator {
    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            throw new Error("Test Bug: Expected region size wasn't provided as command line argument");
        }

        long regionSize = Long.parseLong(args[0]) * 1024 * 1024;

        Path wrkDir = Paths.get("");
        generateClassLoader(regionSize, wrkDir);

    }

    public static void generateClassLoader(long regionSize, Path wrkDir) throws IOException {
        // Generating simple classloader
        String finalSimpleClassLoaderPrototype = TestHumongousClassLoader.GENERIC_PROTOTYPE
                .replace("${Methods}",
                        TestHumongousClassLoader.LOAD_CLASS_METHOD_PROTOTYPE
                                .replace("${ClassLoadFilter}",
                                        "fileName.equals(\"" + TestHumongousClassLoader.HUMONGOUS_CLASSLOADER_NAME
                                                + "\")"))
                .replace("${ClassHeader}", TestHumongousClassLoader.CLASS_HEADER)
                .replace("${ConstructorClause}", TestHumongousClassLoader.CONSTUCTOR_PROTOTYPE);

        Helpers.generateByTemplateAndCompile(TestHumongousClassLoader.SIMPLE_CLASSLOADER_NAME, "ClassLoader",
                finalSimpleClassLoaderPrototype, TestHumongousClassLoader.CONSTUCTOR_PROTOTYPE, regionSize / 4,
                wrkDir, TestHumongousClassLoader.SIMPLE_CLASSLOADER_NAME + "Base");


        // Preparations for generating humongous classloader

        // Generating condition for loadClass method of generated class loader
        // We want the generated class loader to load only classes from G1SampleClass enum
        // All other classes should be loaded by parent classloader
        // As result we get full loadClass method
        StringBuilder classesToLoadBuilder = new StringBuilder();
        for (G1SampleClass g1SampleClass : G1SampleClass.values()) {
            if (classesToLoadBuilder.length() != 0) {
                classesToLoadBuilder.append(" || ");
            }
            classesToLoadBuilder.append("fileName.startsWith(\"" + Helpers.enumNameToClassName(g1SampleClass.name())
                    + "\")");
        }

        // Generating final class loader prototype - with specified methods,header and constructor
        String finalHumongousClassLoaderPrototype = TestHumongousClassLoader.GENERIC_PROTOTYPE
                .replace("${Methods}",
                        TestHumongousClassLoader.LOAD_CLASS_METHOD_PROTOTYPE
                                .replace("${ClassLoadFilter}", classesToLoadBuilder))
                .replace("${ClassHeader}", TestHumongousClassLoader.CLASS_HEADER)
                .replace("${ConstructorClause}", TestHumongousClassLoader.CONSTUCTOR_PROTOTYPE);


        // Generating humongous classloader with specified name, base class, final class prototype and
        // constructor prototype for filler classes
        // Generated class instance should be humongous since we specify size of (regionSize * 3 / 4)
        Helpers.generateByTemplateAndCompile(TestHumongousClassLoader.HUMONGOUS_CLASSLOADER_NAME, "ClassLoader",
                finalHumongousClassLoaderPrototype, TestHumongousClassLoader.CONSTUCTOR_PROTOTYPE,
                regionSize * 3 / 4,
                wrkDir, TestHumongousClassLoader.HUMONGOUS_CLASSLOADER_NAME + "Base");


    }
}
