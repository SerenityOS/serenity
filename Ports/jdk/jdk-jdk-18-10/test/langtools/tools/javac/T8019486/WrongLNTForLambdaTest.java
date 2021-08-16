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
 * @bug 8019486 8026861 8027142
 * @summary javac, generates erroneous LVT for a test case with lambda code
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main WrongLNTForLambdaTest
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

public class WrongLNTForLambdaTest {

    static final String testSource =
    /* 01 */        "import java.util.List;\n" +
    /* 02 */        "import java.util.Arrays;\n" +
    /* 03 */        "import java.util.stream.Collectors;\n" +
    /* 04 */        "\n" +
    /* 05 */        "public class Foo {\n" +
    /* 06 */        "    void bar(int value) {\n" +
    /* 07 */        "        final List<Integer> numbers = Arrays.asList(1, 2, 3);\n" +
    /* 08 */        "        final List<Integer> numbersPlusOne = \n" +
    /* 09 */        "             numbers.stream().map(number -> number / 1).collect(Collectors.toList());\n" +
    /* 10 */        "    }\n" +
    /* 11 */        "    void variablesInLambdas(int value) {\n" +
    /* 12 */        "        Runnable r1 = () -> {\n" +
    /* 13 */        "            int i  = value;\n" +
    /* 14 */        "            class FooBar<T extends CharSequence> {\n" +
    /* 15 */        "                public void run() {\n" +
    /* 16 */        "                    T t = null;\n" +
    /* 17 */        "                }\n" +
    /* 18 */        "            }\n" +
    /* 19 */        "        };\n" +
    /* 20 */        "        Runnable r2 = () -> System.err.println(1);\n" +
    /* 21 */        "        Runnable r3 = (Runnable & java.io.Serializable) this::foo;\n" +
    /* 22 */        "        Runnable r4 = super :: notify;\n" +
    /* 23 */        "    }\n" +
    /* 24 */        "    private void foo() {}\n" +
    /* 25 */        "    void assignLambda() {\n" +
    /* 26 */        "        Runnable r = () -> { };\n" +
    /* 27 */        "    }\n" +
    /* 28 */        "    void callLambda(int i, Runnable r) {\n" +
    /* 29 */        "        callLambda(0,\n" +
    /* 30 */        "                   () -> { });\n" +
    /* 31 */        "    }\n" +
    /* 32 */        "}";

    static final int[][] simpleLambdaExpectedLNT = {
    //  {line-number, start-pc},
        {9,           0},       //number -> number / 1
    };

    static final int[][] lambdaWithVarsExpectedLNT = {
    //  {line-number, start-pc},
        {13,           0},       //number -> number / 1
        {19,           2},       //number -> number / 1
    };

    static final int[][] insideLambdaWithVarsExpectedLNT = {
    //  {line-number, start-pc},
        {16,           0},       //number -> number / 1
        {17,           2},       //number -> number / 1
    };

    static final int[][] lambdaVoid2VoidExpectedLNT = {
    //  {line-number, start-pc},
        {20,           0},       //number -> number / 1
    };

    static final int[][] deserializeExpectedLNT = {
    //  {line-number, start-pc},
        {05,           0},       //number -> number / 1
    };

    static final int[][] lambdaBridgeExpectedLNT = {
    //  {line-number, start-pc},
        {22,           0},       //number -> number / 1
    };

    static final int[][] assignmentExpectedLNT = {
    //  {line-number, start-pc},
        {26,           0},       //number -> number / 1
        {27,           6},       //number -> number / 1
    };

    static final int[][] callExpectedLNT = {
    //  {line-number, start-pc},
        {29,           0},       //number -> number / 1
        {31,           10},       //number -> number / 1
    };

    public static void main(String[] args) throws Exception {
        new WrongLNTForLambdaTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Foo.class").toUri()), "lambda$bar$0", simpleLambdaExpectedLNT);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Foo.class").toUri()), "lambda$variablesInLambdas$1", lambdaWithVarsExpectedLNT);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Foo$1FooBar.class").toUri()), "run", insideLambdaWithVarsExpectedLNT);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Foo.class").toUri()), "lambda$variablesInLambdas$2", lambdaVoid2VoidExpectedLNT);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Foo.class").toUri()), "$deserializeLambda$", deserializeExpectedLNT);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Foo.class").toUri()), "lambda$variablesInLambdas$3", lambdaBridgeExpectedLNT);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Foo.class").toUri()), "assignLambda", assignmentExpectedLNT);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Foo.class").toUri()), "callLambda", callExpectedLNT);
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .sources(testSource)
                .run();
    }

    void checkClassFile(final File cfile, String methodToFind, int[][] expectedLNT) throws Exception {
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
