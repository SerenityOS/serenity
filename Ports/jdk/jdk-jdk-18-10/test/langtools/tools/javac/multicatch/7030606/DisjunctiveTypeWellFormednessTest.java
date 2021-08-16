/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7030606 8006694 8129962
 * @summary Project-coin: multi-catch types should be pairwise disjoint
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main DisjunctiveTypeWellFormednessTest
 */

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;


public class DisjunctiveTypeWellFormednessTest extends ComboInstance<DisjunctiveTypeWellFormednessTest> {

    enum Alternative implements ComboParameter {
        EXCEPTION("Exception"),
        RUNTIME_EXCEPTION("RuntimeException"),
        IO_EXCEPTION("java.io.IOException"),
        FILE_NOT_FOUND_EXCEPTION("java.io.FileNotFoundException"),
        ILLEGAL_ARGUMENT_EXCEPTION("IllegalArgumentException");

        String exceptionStr;

        Alternative(String exceptionStr) {
            this.exceptionStr = exceptionStr;
        }

        boolean disjoint(Alternative that) {
            return disjoint[this.ordinal()][that.ordinal()];
        }

        static boolean[][] disjoint = {
            //                              Exception    RuntimeException    IOException    FileNotFoundException    IllegalArgumentException
            /*Exception*/                {  false,       false,              false,         false,                   false },
            /*RuntimeException*/         {  false,       false,              true,          true,                    false },
            /*IOException*/              {  false,       true,               false,         false,                   true },
            /*FileNotFoundException*/    {  false,       true,               false,         false,                   true },
            /*IllegalArgumentException*/ {  false,       false,              true,          true,                    false }
        };

        @Override
        public String expand(String optParameter) {
            return exceptionStr;
        }
    }

    enum Arity implements ComboParameter {
        ONE(1, "#{TYPE[0]}"),
        TWO(2, "#{TYPE[0]} | #{TYPE[1]}"),
        THREE(3, "#{TYPE[0]} | #{TYPE[1]} | #{TYPE[2]}"),
        FOUR(4, "#{TYPE[0]} | #{TYPE[1]} | #{TYPE[2]} | #{TYPE[3]}"),
        FIVE(5, "#{TYPE[0]} | #{TYPE[1]} | #{TYPE[2]} | #{TYPE[3]} | #{TYPE[4]}");

        int n;
        String arityTemplate;

        Arity(int n, String arityTemplate) {
            this.n = n;
            this.arityTemplate = arityTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return arityTemplate;
        }
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<DisjunctiveTypeWellFormednessTest>()
                .withFilter(DisjunctiveTypeWellFormednessTest::arityFilter)
                .withDimension("CTYPE", (x, arity) -> x.arity = arity, Arity.values())
                .withArrayDimension("TYPE", (x, type, idx) -> x.alternatives[idx] = type, 5, Alternative.values())
                .run(DisjunctiveTypeWellFormednessTest::new);
    }

    Arity arity;
    Alternative[] alternatives = new Alternative[5];

    boolean arityFilter() {
        for (int i = arity.n; i < alternatives.length ; i++) {
            if (alternatives[i].ordinal() != 0) {
                return false;
            }
        }
        return true;
    }

    String template = "class Test {\n" +
                      "void test() {\n" +
                      "try {} catch (#{CTYPE} e) {}\n" +
                      "}\n" +
                      "}\n";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(template)
                .analyze(this::check);
    }

    void check(Result<?> res) {

        int non_disjoint = 0;
        for (int i = 0 ; i < arity.n ; i++) {
            for (int j = 0 ; j < i ; j++) {
                if (!alternatives[i].disjoint(alternatives[j])) {
                    non_disjoint++;
                    break;
                }
            }
        }

        int foundErrs = res.diagnosticsForKey("compiler.err.multicatch.types.must.be.disjoint").size();
        if (non_disjoint != foundErrs) {
            fail("invalid diagnostics for source:\n" +
                    res.compilationInfo() +
                    "\nFound errors: " + foundErrs +
                    "\nExpected errors: " + non_disjoint);
        }
    }
}
