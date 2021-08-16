/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8029240 8030855
 * @summary Default methods not always visible under -source 7
 * Default methods should be visible under source previous to 8
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main DefaultMethodsNotVisibleForSourceLessThan8Test
 */

import java.nio.file.Files;
import java.nio.file.Paths;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class DefaultMethodsNotVisibleForSourceLessThan8Test {
    // common definitions

    // this one should be compiled with source 8, the rest with source < 8
    static final String ISrc =
        "interface I {\n" +
        "    default void m() {}\n" +
        "}";

    static final String JSrc =
        "interface J extends I {}";

    static final String ASrc =
        "abstract class A implements I {}";

    static final String BSrc =
        "class B implements I {}";

    // test legacy implementations
    static final String C1Src =
        "class C1 implements I {\n" +
        "    public void m() {}\n" +
        "}";

    static final String C2Src =
        "class C2 implements J {\n" +
        "    public void m() {}\n" +
        "}";

    static final String C3Src =
        "class C3 extends A {\n" +
        "    public void m() {}\n" +
        "}";

    static final String C4Src =
        "class C4 extends B {\n" +
        "    public void m() {}\n" +
        "}";

    //test legacy invocations
    static final String LegacyInvocationSrc =
        "class LegacyInvocation {\n" +
        "    public static void test(I i, J j, A a, B b) {\n" +
        "        i.m();\n" +
        "        j.m();\n" +
        "        a.m();\n" +
        "        b.m();\n" +
        "    }\n" +
        "}";

    //test case super invocations
    static final String SubASrc =
        "class SubA extends A {\n" +
        "    public void test() {\n" +
        "        super.m();\n" +
        "    }\n" +
        "}";

    static final String SubBSrc =
        "class SubB extends B {\n" +
        "    public void test() {\n" +
        "        super.m();\n" +
        "    }\n" +
        "}";

    public static void main(String[] args) throws Exception {
        String[] sources = new String[] {
            "1.7",
        };
        for (String source : sources) {
            new DefaultMethodsNotVisibleForSourceLessThan8Test().run(source);
        }
    }

    String outDir;
    String source;
    ToolBox tb = new ToolBox();

    void run(String source) throws Exception {
        this.source = source;
        outDir = "out" + source.replace('.', '_');
        testsPreparation();
        testLegacyImplementations();
        testLegacyInvocations();
        testSuperInvocations();
    }

    void testsPreparation() throws Exception {
        Files.createDirectory(Paths.get(outDir));

        /* as an extra check let's make sure that interface 'I' can't be compiled
         * with source < 8
         */
        new JavacTask(tb)
                .outdir(outDir)
                .options("-source", source)
                .sources(ISrc)
                .run(Task.Expect.FAIL);

        //but it should compile with source >= 8
        new JavacTask(tb)
                .outdir(outDir)
                .sources(ISrc)
                .run();

        new JavacTask(tb)
                .outdir(outDir)
                .classpath(outDir)
                .options("-source", source)
                .sources(JSrc, ASrc, BSrc)
                .run();
    }

    void testLegacyImplementations() throws Exception {
        //compile C1-4
        new JavacTask(tb)
                .outdir(outDir)
                .classpath(outDir)
                .options("-source", source)
                .sources(C1Src, C2Src, C3Src, C4Src)
                .run();
    }

    void testLegacyInvocations() throws Exception {
        //compile LegacyInvocation
        new JavacTask(tb)
                .outdir(outDir)
                .classpath(outDir)
                .options("-source", source)
                .sources(LegacyInvocationSrc)
                .run();
    }

    void testSuperInvocations() throws Exception {
        //compile SubA, SubB
        new JavacTask(tb)
                .outdir(outDir)
                .classpath(outDir)
                .options("-source", source)
                .sources(SubASrc, SubBSrc)
                .run();
    }
}
