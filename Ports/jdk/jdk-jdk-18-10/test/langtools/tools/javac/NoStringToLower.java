/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8029800
 * @summary String.toLowerCase()/toUpperCase is generally dangerous, check it is not used in langtools
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import java.io.*;
import java.util.*;
import javax.tools.*;
import com.sun.tools.classfile.*;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Methodref_info;

public class NoStringToLower {
    public static void main(String... args) throws Exception {
        NoStringToLower c = new NoStringToLower();
        if (c.run(args))
            return;

        if (is_jtreg())
            throw new Exception(c.errors + " errors occurred");
        else
            System.exit(1);
    }

    static boolean is_jtreg() {
        return (System.getProperty("test.src") != null);
    }

    /**
     * Main entry point.
     */
    boolean run(String... args) throws Exception {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (JavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            JavaFileManager.Location javacLoc = findJavacLocation(fm);
            String[] pkgs = {
                "javax.annotation.processing",
                "javax.lang.model",
                "javax.tools",
                "com.sun.source",
                "com.sun.tools.classfile",
                "com.sun.tools.doclint",
                "com.sun.tools.javac",
                "com.sun.tools.javah",
                "com.sun.tools.javap",
                "com.sun.tools.jdeps",
                "com.sun.tools.sjavac",
                "jdk.javadoc"
            };
            for (String pkg: pkgs) {
                for (JavaFileObject fo: fm.list(javacLoc,
                        pkg, EnumSet.of(JavaFileObject.Kind.CLASS), true)) {
                    scan(fo);
                }
            }

            return (errors == 0);
        }
    }

    // depending on how the test is run, javac may be on bootclasspath or classpath
    JavaFileManager.Location findJavacLocation(JavaFileManager fm) {
        JavaFileManager.Location[] locns =
            { StandardLocation.PLATFORM_CLASS_PATH, StandardLocation.CLASS_PATH };
        try {
            for (JavaFileManager.Location l: locns) {
                JavaFileObject fo = fm.getJavaFileForInput(l,
                    "com.sun.tools.javac.Main", JavaFileObject.Kind.CLASS);
                if (fo != null)
                    return l;
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        throw new IllegalStateException("Cannot find javac");
    }

    /**
     * Verify there are no references to String.toLowerCase() in a class file.
     */
    void scan(JavaFileObject fo) throws IOException {
        InputStream in = fo.openInputStream();
        try {
            ClassFile cf = ClassFile.read(in);
            for (ConstantPool.CPInfo cpinfo: cf.constant_pool.entries()) {
                if (cpinfo.getTag() == ConstantPool.CONSTANT_Methodref) {
                    CONSTANT_Methodref_info ref = (CONSTANT_Methodref_info) cpinfo;
                    String methodDesc = ref.getClassInfo().getName() + "." + ref.getNameAndTypeInfo().getName() + ":" + ref.getNameAndTypeInfo().getType();

                    if ("java/lang/String.toLowerCase:()Ljava/lang/String;".equals(methodDesc)) {
                        error("found reference to String.toLowerCase() in: " + fo.getName());
                    }
                    if ("java/lang/String.toUpperCase:()Ljava/lang/String;".equals(methodDesc)) {
                        error("found reference to String.toLowerCase() in: " + fo.getName());
                    }
                }
            }
        } catch (ConstantPoolException ignore) {
        } finally {
            in.close();
        }
    }

    /**
     * Report an error.
     */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
