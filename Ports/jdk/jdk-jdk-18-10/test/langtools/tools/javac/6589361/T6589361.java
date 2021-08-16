/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     6589361
 * @summary 6589361:Failing building ct.sym file as part of the control build
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.File;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import java.util.Set;
import java.util.HashSet;

public class T6589361 {
    public static void main(String [] args) throws Exception {
        JavacFileManager fm = null;
        try {
            fm = new JavacFileManager(new Context(), false, null);
            Set<JavaFileObject.Kind> set = new HashSet<JavaFileObject.Kind>();
            set.add(JavaFileObject.Kind.CLASS);
            Iterable<JavaFileObject> files = fm.list(StandardLocation.PLATFORM_CLASS_PATH, "java.lang", set, false);
            for (JavaFileObject file : files) {
                // Note: Zip/Jar entry names use '/', not File.separator, but just to be sure,
                // we normalize the filename as well.
                if (file.getName().replace(File.separatorChar, '/').contains("java/lang/Object.class")) {
                    String str = fm.inferBinaryName(StandardLocation.PLATFORM_CLASS_PATH, file);
                    if (!str.equals("java.lang.Object")) {
                        System.err.println("file object: " + file);
                        System.err.println("      class: " + file.getClass());
                        System.err.println("       name: " + file.getName());
                        System.err.println("binary name: " + str);
                        throw new AssertionError("Error in JavacFileManager.inferBinaryName method!");
                    }
                    else {
                        return;
                    }
                }
            }
        }
        finally {
            if (fm != null) {
                fm.close();
            }
        }
        throw new AssertionError("Could not find java/lang/Object.class while compiling");
    }

}
