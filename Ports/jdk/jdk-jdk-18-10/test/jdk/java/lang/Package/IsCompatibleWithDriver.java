/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4227825 4785473
 * @summary Test behaviour of Package.isCompatibleWith().
 * @library /test/lib
 * @build A IsCompatibleWith
 *        jdk.test.lib.util.JarUtils
 *        jdk.test.lib.process.*
 * @run main IsCompatibleWithDriver
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;

import static java.io.File.pathSeparator;

import java.io.InputStream;
import java.nio.file.Files;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

public class IsCompatibleWithDriver {
    public static void main(String args[]) throws Throwable {
        Path classes = Paths.get(System.getProperty("test.classes", ""));
        Path manifest = Paths.get(System.getProperty("test.src"), "test.mf");
        try (InputStream is = Files.newInputStream(manifest)) {
            JarUtils.createJarFile(Paths.get("test.jar"), new Manifest(is),
                    classes, classes.resolve("p"));
        }
        Files.delete(classes.resolve("p").resolve("A.class"));

        OutputAnalyzer analyzer = ProcessTools.executeTestJava("-cp",
                "test.jar" + pathSeparator + classes.toString(), "IsCompatibleWith");
        System.out.println(analyzer.getOutput());
        analyzer.shouldHaveExitValue(0);
    }
}
