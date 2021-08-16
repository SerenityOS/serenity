/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6440528
 * @summary javac deposits package-info.class in bogus directory
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler/com.sun.tools.javac.file
 * @build ToolTester
 * @compile T6440528.java
 * @run main T6440528
 */

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import javax.tools.*;

public class T6440528 extends ToolTester {
    void test(String... args) throws Exception {
        fm.setLocation(CLASS_OUTPUT, null); // no class files are
                                            // generated, so this will
                                            // not leave clutter in
                                            // the source directory
        Iterable<File> files = Arrays.asList(new File(test_src, "package-info.java"));
        JavaFileObject src = fm.getJavaFileObjectsFromFiles(files).iterator().next();
        char sep = File.separatorChar;
        FileObject cls = fm.getFileForOutput(CLASS_OUTPUT,
                                             "com.sun.foo.bar.baz",
                                             "package-info.class",
                                             src);
        File expect = new File(test_src, "package-info.class");
        File got = fm.asPath(cls).toFile();
        if (!got.equals(expect))
            throw new AssertionError(String.format("Expected: %s; got: %s", expect, got));
        System.err.println("Expected: " + expect);
        System.err.println("Got:      " + got);
    }

    public static void main(String... args) throws Exception {
        try (T6440528 t = new T6440528()) {
            t.test(args);
        }
    }
}
