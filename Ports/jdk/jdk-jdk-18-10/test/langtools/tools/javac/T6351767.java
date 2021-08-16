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
 * @bug 6351767
 * @summary javax.tools.JavaCompilerTool.getStandardFileManager().list() treats directories as package
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

public class T6351767 {
    public static void main(String... args) throws Exception {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (JavaFileManager jfm = compiler.getStandardFileManager(null, null, null)) {

            // test null
            try {
                jfm.list(StandardLocation.SOURCE_PATH, null, EnumSet.of(Kind.SOURCE), false);
                error("NPE not thrown");
            }
            catch (NullPointerException e) {
                // expected
            }

            // test null fileKinds
            try {
                jfm.list(StandardLocation.SOURCE_PATH, "", null, false);
                error("NPE not thrown");
            }
            catch (NullPointerException e) {
                // expected
            }

            // test good package
            boolean found = false;
            for (JavaFileObject jfo : jfm.list(StandardLocation.PLATFORM_CLASS_PATH,
                                               "java.lang",
                                               EnumSet.of(Kind.CLASS),
                                               false)) {
                System.err.println("found " + jfo.toUri());
                if (jfo.isNameCompatible("Object", Kind.CLASS))
                    found = true;
            }
            if (!found)
                error("expected file, java/lang/Object.class, not found");

            found = false;
            // test good package (VM name)
            for (JavaFileObject jfo : jfm.list(StandardLocation.PLATFORM_CLASS_PATH,
                                               "java/lang",
                                               EnumSet.of(Kind.CLASS),
                                               false)) {
                System.err.println("found " + jfo.toUri());
                if (jfo.isNameCompatible("Object", Kind.CLASS))
                    found = true;
            }
            if (!found)
                error("expected file, java/lang/Object.class, not found");
        }
    }

    static void error(String msg) {
        throw new AssertionError(msg);
    }
}
