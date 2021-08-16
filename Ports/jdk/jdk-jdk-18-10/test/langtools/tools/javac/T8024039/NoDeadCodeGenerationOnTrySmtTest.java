/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024039
 * @summary javac, previous solution for JDK-8022186 was incorrect
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main NoDeadCodeGenerationOnTrySmtTest
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.Code_attribute.Exception_data;
import com.sun.tools.classfile.Method;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class NoDeadCodeGenerationOnTrySmtTest {

    static final String testSource =
        "public class Test {\n" +
        "    void m1(int arg) {\n" +
        "        synchronized (new Integer(arg)) {\n" +
        "            {\n" +
        "                label0:\n" +
        "                do {\n" +
        "                    break label0;\n" +
        "                } while (arg != 0);\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +

        "    void m2(int arg) {\n" +
        "        synchronized (new Integer(arg)) {\n" +
        "            {\n" +
        "                label0:\n" +
        "                {\n" +
        "                    break label0;\n" +
        "                }\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "}";

    static final int[][] expectedExceptionTable = {
    //  {from,         to,         target,      type},
        {11,           13,         16,          0},
        {16,           19,         16,          0}
    };

    static final String[] methodsToLookFor = {"m1", "m2"};

    ToolBox tb = new ToolBox();

    public static void main(String[] args) throws Exception {
        new NoDeadCodeGenerationOnTrySmtTest().run();
    }

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Test.class").toUri()), methodsToLookFor);
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .sources(testSource)
                .run();
    }

    void checkClassFile(final File cfile, String[] methodsToFind) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        int numberOfmethodsFound = 0;
        for (String methodToFind : methodsToFind) {
            for (Method method : classFile.methods) {
                if (method.getName(classFile.constant_pool).equals(methodToFind)) {
                    numberOfmethodsFound++;
                    Code_attribute code = (Code_attribute) method.attributes.get("Code");
                    Assert.check(code.exception_table_length == expectedExceptionTable.length,
                            "The ExceptionTable found has a length different to the expected one");
                    int i = 0;
                    for (Exception_data entry: code.exception_table) {
                        Assert.check(entry.start_pc == expectedExceptionTable[i][0] &&
                                entry.end_pc == expectedExceptionTable[i][1] &&
                                entry.handler_pc == expectedExceptionTable[i][2] &&
                                entry.catch_type == expectedExceptionTable[i][3],
                                "Exception table entry at pos " + i + " differ from expected.");
                        i++;
                    }
                }
            }
        }
        Assert.check(numberOfmethodsFound == 2, "Some seek methods were not found");
    }

    void error(String msg) {
        throw new AssertionError(msg);
    }

}
