/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Simple jar builder
 *   Input: jarName className1 className2 ...
 *     do not specify extensions, just the names
 *     E.g. prot_domain ProtDomainA ProtDomainB
 *   Output: A jar containing compiled classes, placed in a test classes folder
 */

import java.io.File;
import java.util.ArrayList;
import java.util.spi.ToolProvider;

// Using JarBuilder requires that all to-be-jarred classes should be placed
// in the current working directory, aka "."
public class BasicJarBuilder {
    private static final String classDir = System.getProperty("test.classes");

    private static final ToolProvider JAR = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new RuntimeException("ToolProvider for jar not found"));

    public static void build(boolean classesInWorkDir, String jarName,
        String ...classNames) throws Exception {

        if (classesInWorkDir) {
            createSimpleJar(".", classDir + File.separator + jarName + ".jar", classNames);
        } else {
            build(jarName, classNames);
        }
    }

    public static void build(String jarName, String ...classNames) throws Exception {
        createSimpleJar(classDir, classDir + File.separator + jarName + ".jar",
            classNames);
    }

    private static void createSimpleJar(String jarclassDir, String jarName,
        String[] classNames) throws Exception {
        ArrayList<String> args = new ArrayList<String>();
        args.add("cf");
        args.add(jarName);
        addClassArgs(args, jarclassDir, classNames);
        createJar(args);
    }

    private static void addClassArgs(ArrayList<String> args, String jarclassDir,
        String[] classNames) {

        for (String name : classNames) {
            args.add("-C");
            args.add(jarclassDir);
            args.add(name + ".class");
        }
    }

    private static void createJar(ArrayList<String> args) {
        if (JAR.run(System.out, System.err, args.toArray(new String[1])) != 0) {
            throw new RuntimeException("jar operation failed");
        }
    }

    // Get full path to the test jar
    public static String getTestJar(String jar) {
        File dir = new File(System.getProperty("test.classes", "."));
        File jarFile = new File(dir, jar);
        if (!jarFile.exists()) {
            throw new RuntimeException("Cannot find " + jarFile.getPath());
        }
        if (!jarFile.isFile()) {
            throw new RuntimeException("Not a regular file: " + jarFile.getPath());
        }
        return jarFile.getPath();
    }
}
