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
 * @bug     6440333
 * @summary SimpleJavaFileObject.toString() generates URI with some extra message
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler
 * @build ToolTester
 * @compile T6440333.java
 * @run main T6440333
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import javax.tools.JavaFileObject;

public class T6440333 extends ToolTester {
    void test(String... args) throws IOException {
        File path = test_src.getCanonicalFile();
        File src = new File(new File(path, "."), "T6440333.java");
        JavaFileObject fo = fm.getJavaFileObjects(src).iterator().next();
        URI expect = src.getCanonicalFile().toURI();
        System.err.println("Expect " + expect);
        System.err.println("Found  " + fo.toUri());
        if (!expect.equals(fo.toUri())) {
            throw new AssertionError();
        }
    }
    public static void main(String... args) throws IOException {
        try (T6440333 t = new T6440333()) {
            t.test(args);
        }
    }
}
