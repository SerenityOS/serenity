/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6665791
 * @summary com.sun.source.tree.MethodTree.toString() does not output default values
 * @modules jdk.compiler
 */

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.io.FileWriter;

public class T6665791 {
    static String test = "public @interface Annotation { boolean booleanProperty() default false; }";
    static File test_java = new File("Test.java");

    public static void main(String[] args) throws Exception {
        write(test_java, test);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager manager =
                compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> units = manager.getJavaFileObjects(test_java);
            final StringWriter sw = new StringWriter();
            JavacTask task = (JavacTask) compiler.getTask(sw, manager, null, null,
                    null, units);

            new TreeScanner<Boolean, Void>() {
                @Override
                public Boolean visitClass(ClassTree arg0, Void arg1) {
                    sw.write(arg0.toString());
                    return super.visitClass(arg0, arg1);
                }
            }.scan(task.parse(), null);

            System.out.println("output:");
            System.out.println(sw.toString());
            String found = sw.toString().replaceAll("\\s+", " ").trim();
            String expect = test.replaceAll("\\s+", " ").trim();
            if (!expect.equals(found)) {
                System.out.println("expect: " + expect);
                System.out.println("found:  " + found);
                throw new Exception("unexpected output");
            }
        }
    }

    static void write(File file, String body) throws IOException {
        FileWriter out = new FileWriter(file);
        out.write(body);
        out.close();
    }
}
