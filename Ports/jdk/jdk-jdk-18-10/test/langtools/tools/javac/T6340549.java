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
 * @test 6403456
 * @summary javax.tools.JavaCompilerTool.getStandardFileManager().list() includes directories
 * @modules java.compiler
 *          jdk.compiler
 */

import javax.tools.*;
import java.util.*;
import java.io.*;

import static javax.tools.JavaFileObject.Kind;

public class T6340549 {
    public static void main(String... args) throws Exception {

        // Ensure a directory exists
        File dir = new File("temp" + args.hashCode());
        if (!dir.exists())
            dir.mkdir();
        if (!dir.isDirectory())
            throw new AssertionError("Not a directory " + dir);

        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            try (StandardJavaFileManager jfm = compiler.getStandardFileManager(null, null, null)) {
                jfm.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(new File(".")));

                for (JavaFileObject jfo : jfm.list(StandardLocation.CLASS_PATH,
                        "", EnumSet.of(Kind.OTHER), false)) {
                    if (new File(jfo.getName()).isDirectory()) {
                        throw new AssertionError("Found directory: " + jfo);
                    }
                }
            }
        } finally {
            dir.delete(); // cleanup
        }
    }
}
