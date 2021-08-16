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
 * @bug     6420464
 * @summary JSR 199: JavaFileObject.isNameCompatible throws unspecified exception (IllegalArgumentException)
 * @author  Igor Tseytin
 * @modules java.compiler
 *          jdk.compiler
 */

import javax.tools.*;
import java.io.*;
import java.util.Collections;

public class T6420464 {
    static final File test_src     = new File(System.getProperty("test.src"));
    static final File test_classes = new File(System.getProperty("test.classes"));

    public static void main(String... args) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager mgr = compiler.getStandardFileManager(null, null, null)) {
            mgr.setLocation(StandardLocation.SOURCE_PATH, Collections.singleton(test_src));
            JavaFileObject f = mgr.getJavaFileForInput(StandardLocation.SOURCE_PATH,
                                                       "T6420464",
                                                       JavaFileObject.Kind.SOURCE);
            if (!f.isNameCompatible("T6420464", JavaFileObject.Kind.SOURCE))
                throw new AssertionError("isNameCompatible(SOURCE) fails on " + f.toUri());
            if (f.isNameCompatible("T6420464", JavaFileObject.Kind.OTHER))
                throw new AssertionError("isNameCompatible(OTHER) fails on " + f.toUri());
            System.out.println("OK");
        }
    }
}
