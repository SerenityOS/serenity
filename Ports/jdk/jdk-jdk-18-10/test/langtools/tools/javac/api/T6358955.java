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
 * @bug 6358955
 * @summary JavacFileManager.getFileForInput(dir) should throw IAE
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import javax.tools.*;
import static javax.tools.JavaFileObject.Kind.*;

public class T6358955 {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager jfm = compiler.getStandardFileManager(null, null, null)) {

            File dir = new File("temp" + args.hashCode());
            if (!dir.exists())
                dir.mkdir();
            if (!dir.isDirectory())
                throw new AssertionError("Not a directory " + dir);

            try {
                jfm.setLocation(StandardLocation.CLASS_OUTPUT,
                                Arrays.asList(dir.getCanonicalFile().getParentFile()));
                try {
                    jfm.getFileForInput(StandardLocation.CLASS_OUTPUT, "", dir.getPath());
                    throw new AssertionError("IllegalArgumentException not thrown");
                } catch (IllegalArgumentException e) {
                    System.out.println("OK: " + e.getLocalizedMessage());
                }
                try {
                    jfm.getJavaFileObjectsFromFiles(Arrays.asList(dir));
                    throw new AssertionError("IllegalArgumentException not thrown");
                } catch (IllegalArgumentException e) {
                    System.out.println("OK: " + e.getLocalizedMessage());
                }
            } finally {
                try {
                    dir.delete(); // cleanup
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}
