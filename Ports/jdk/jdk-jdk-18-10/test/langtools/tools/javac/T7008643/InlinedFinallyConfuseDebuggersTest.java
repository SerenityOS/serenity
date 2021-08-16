/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7008643
 * @summary inlined finally clauses confuse debuggers
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main InlinedFinallyConfuseDebuggersTest
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.LineNumberTable_attribute;
import com.sun.tools.classfile.Method;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class InlinedFinallyConfuseDebuggersTest {

    static final String testSource =
    /* 01 */        "public class InlinedFinallyTest {\n" +
    /* 02 */        "    void lookForThisMethod(int value) {\n" +
    /* 03 */        "        try {\n" +
    /* 04 */        "            if (value > 0) {\n" +
    /* 05 */        "                System.out.println(\"if\");\n" +
    /* 06 */        "                return;\n" +
    /* 07 */        "            }\n" +
    /* 08 */        "        } finally {\n" +
    /* 09 */        "            System.out.println(\"finally\");\n" +
    /* 10 */        "        }\n" +
    /* 11 */        "    }\n" +
    /* 12 */        "}";

    static final int[][] expectedLNT = {
    //  {line-number, start-pc},
        {4,           0},       //if (value > 0) {
        {5,           4},       //    System.out.println("if");
        {9,           12},      //System.out.println("finally");
        {6,           20},      //    return;
        {9,           21},      //System.out.println("finally");
        {10,          29},
        {9,           32},      //System.out.println("finally");
        {10,          41},      //}
        {11,          43},
    };

    static final String methodToLookFor = "lookForThisMethod";

    public static void main(String[] args) throws Exception {
        new InlinedFinallyConfuseDebuggersTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "InlinedFinallyTest.class").toUri()), methodToLookFor);
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .sources(testSource)
                .run();
    }

    void checkClassFile(final File cfile, String methodToFind) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        boolean methodFound = false;
        for (Method method : classFile.methods) {
            if (method.getName(classFile.constant_pool).equals(methodToFind)) {
                methodFound = true;
                Code_attribute code = (Code_attribute) method.attributes.get("Code");
                LineNumberTable_attribute lnt =
                        (LineNumberTable_attribute) code.attributes.get("LineNumberTable");
                Assert.check(lnt.line_number_table_length == expectedLNT.length,
                        "The LineNumberTable found has a length different to the expected one");
                int i = 0;
                for (LineNumberTable_attribute.Entry entry: lnt.line_number_table) {
                    Assert.check(entry.line_number == expectedLNT[i][0] &&
                            entry.start_pc == expectedLNT[i][1],
                            "LNT entry at pos " + i + " differ from expected." +
                            "Found " + entry.line_number + ":" + entry.start_pc +
                            ". Expected " + expectedLNT[i][0] + ":" + expectedLNT[i][1]);
                    i++;
                }
            }
        }
        Assert.check(methodFound, "The seek method was not found");
    }

    void error(String msg) {
        throw new AssertionError(msg);
    }

}
