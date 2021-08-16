/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8067422
 * @summary Check that the lambda names are not unnecessarily unstable
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main TestNonSerializableLambdaNameStability
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.tools.StandardLocation;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Method;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class TestNonSerializableLambdaNameStability {

    public static void main(String... args) throws Exception {
        new TestNonSerializableLambdaNameStability().run();
    }

    String lambdaSource = "public class L%d {\n" +
                          "    public static class A {\n" +
                          "        private Runnable r = () -> { };\n" +
                          "    }\n" +
                          "    public static class B {\n" +
                          "        private Runnable r = () -> { };\n" +
                          "    }\n" +
                          "    private Runnable r = () -> { };\n" +
                          "}\n";

    String expectedLambdaMethodName = "lambda$new$0";

    void run() throws Exception {
        List<String> sources = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            sources.add(String.format(lambdaSource, i));
        }

        ToolBox tb = new ToolBox();

        try (ToolBox.MemoryFileManager fm = new ToolBox.MemoryFileManager()) {
            new JavacTask(tb)
              .sources(sources.toArray(new String[sources.size()]))
              .fileManager(fm)
              .run();

            for (String file : fm.getFileNames(StandardLocation.CLASS_OUTPUT)) {
                byte[] fileBytes = fm.getFileBytes(StandardLocation.CLASS_OUTPUT, file);
                try (InputStream in = new ByteArrayInputStream(fileBytes)) {
                    boolean foundLambdaMethod = false;
                    ClassFile cf = ClassFile.read(in);
                    StringBuilder seenMethods = new StringBuilder();
                    String sep = "";
                    for (Method m : cf.methods) {
                        String methodName = m.getName(cf.constant_pool);
                        if (expectedLambdaMethodName.equals(methodName)) {
                            foundLambdaMethod = true;
                            break;
                        }
                        seenMethods.append(sep);
                        seenMethods.append(methodName);
                        sep = ", ";
                    }

                    if (!foundLambdaMethod) {
                        throw new AbstractMethodError("Did not find the lambda method, " +
                                                      "found methods: " + seenMethods.toString());
                    }
                }
            }
        }
    }
}
