/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8218419
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 * @run driver GetPackageFromBootClassPath setup
 * @run main/othervm -Xbootclasspath/a:boot GetPackageFromBootClassPath
 */

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class GetPackageFromBootClassPath {
    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            test();
        } else {
            setupBootLib();
        }
    }

    private static void test() throws Exception {
        Class<?> c = Class.forName("foo.Foo", false, null);
        Package p = c.getPackage();
        Annotation[] annotations = p.getAnnotations();
        Class<?> annType = Class.forName("foo.MyAnnotation", false, null);
        if (annotations.length != 1 ||
            annotations[0].annotationType() != annType) {
            throw new RuntimeException("Expected foo.MyAnnotation but got " +
                Arrays.toString(annotations));
        }
    }

    private static void setupBootLib() throws Exception {
        Path testSrc = Paths.get(System.getProperty("test.src"), "boot");
        Path bootDir = Paths.get("boot");
        if (!jdk.test.lib.compiler.CompilerUtils.compile(testSrc, bootDir)) {
            throw new RuntimeException("compilation fails");
        }
    }
}
