/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8258662
 * @summary Types.isCastable crashes when involving sealed interface and type variable.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main T8258662
 */

import java.util.List;

import toolbox.ToolBox;
import toolbox.TestRunner;
import toolbox.JavacTask;

public class T8258662 extends TestRunner {
    private ToolBox tb;

    public T8258662() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        T8258662 t = new T8258662();
        t.runTests();
    }

    @Test
    public void testSealedClassIsCastable() throws Exception {
        String code = """
                class Test8258662 {
                    sealed interface I<T> {
                        final class C implements I<Object> { }
                    }
                    static <T extends I<Object>> void f(T x) {
                        if (x instanceof I<Object>) { }
                    }
                }""";
        new JavacTask(tb)
                .sources(code)
                .classpath(".")
                .run();
    }

}
