/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8255742
 * @summary PrintInlining as compiler directive doesn't print virtual calls
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @requires vm.flagless
 *
 * @run driver compiler.inlining.PrintInlining
 */

package compiler.inlining;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class PrintInlining {

    static void test(String option) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+IgnoreUnrecognizedVMOptions", "-showversion",
                "-server", "-XX:-TieredCompilation", "-Xbatch", "-XX:-UseOnStackReplacement",
                "-XX:CompileCommand=dontinline,*::bar",
                "-XX:CompileCommand=compileonly,*::foo",
                "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", option,
                Launcher.class.getName());

        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());

        analyzer.shouldHaveExitValue(0);

        // The test is applicable only to C2 (present in Server VM).
        if (analyzer.getStderr().contains("Server VM")) {
            analyzer.outputTo(System.out);
            if (analyzer.asLines().stream()
                .filter(s -> s.matches(".*A::bar.+virtual call.*"))
                .count() != 1) {
                throw new Exception("'" + option + "' didn't print virtual call.");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        test("-XX:+PrintInlining");
        test("-XX:CompileCommand=option,*::foo,PrintInlining");
    }

    static class A {
        void bar() {}
    }

    static class B extends A {
        void bar() {}
    }

    static class C extends A {
        void bar() {}
    }

    static class D extends A {
        void bar() {}
    }

    static void foo(A a) {
        a.bar();
    }

    static class Launcher {
        public static void main(String[] args) throws Exception {
            A[] as = { new B(), new C(), new D() };
            for (int i = 0; i < 20_000; i++) {
                foo(as[i % 3]);
            }
        }
    }
}
