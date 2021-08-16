/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6865530
 * @summary ensure JavacFileManager handles non-standard zipfiles.
 * @modules jdk.compiler
 *          jdk.jartool/sun.tools.jar
 * @compile  -XDignore.symbol.file T6865530.java
 * @run main T6865530
 */


import java.io.File;


public class T6865530 {

    public static void main(String... args) throws Exception {
        File badFile = new File("bad.exe");
        File testJar = new File("test.jar");
        File fooJava = new File("Foo.java");
        File barJava = new File("Bar.java");

        // create a jar by compiling a file, and append the jar to some
        // arbitrary data to offset the start of the zip/jar archive
        Utils.createJavaFile(fooJava);
        Utils.compile("-doe", "-verbose", fooJava.getName());
        String[] jarArgs = {
            "cvf", testJar.getAbsolutePath(), "Foo.class"
        };
        Utils.jarTool.run(jarArgs);
        Utils.cat(badFile, fooJava, testJar);

        // create test file and use the above file as a classpath
        Utils.createJavaFile(barJava);
        try {
            if (!Utils.compile("-doe", "-verbose", "-cp", badFile.getAbsolutePath(), "Bar.java")) {
                throw new RuntimeException("test fails javac did not compile");
            }
        } finally {
            Utils.deleteFile(badFile);
            Utils.deleteFile(testJar);
        }
    }
}

