/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6198196 6278523
 * @summary package-info.java: Weird compiler error
 * @author  Peter von der Ah\u00e9
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.IOException;
import java.util.Arrays;
import javax.tools.*;

public class T6198196 {
    static String pkginf = "package-info";
    static void test(String pathname, String filename, boolean result) throws IOException {
        try (StandardJavaFileManager fm =
                ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
            JavaFileObject fo =
                    fm.getJavaFileObjectsFromStrings(Arrays.asList(pathname)).iterator().next();
            if (result != fo.isNameCompatible(filename, JavaFileObject.Kind.SOURCE))
                throw new AssertionError("endsWith(" + pathname + ", "
                                         + filename + ") != " + result);
            System.out.format("OK: endsWith(%s, %s) = %s%n", pathname, filename, result);
        }
    }
    public static void main(String[] args) throws IOException {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        test("/x/y/z/package-info.java", pkginf, true);
        if (windows) {
            test("\\x\\y\\z\\package-info.java", pkginf, true);
            test("..\\x\\y\\z\\package-info.java", pkginf, true);
        } else {
            test("\\x\\y\\z\\package-info.java", pkginf, false);
            test("..\\x\\y\\z\\package-info.java", pkginf, false);
        }
        test("Package-info.java", pkginf, false);
        test("../x/y/z/package-info.java", pkginf, true);
        test("/x/y/z/package-info.java", pkginf, true);
        test("x/y/z/package-info.java", pkginf, true);
        test("package-info.java", pkginf, true);
    }
}
