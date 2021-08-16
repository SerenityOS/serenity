/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8204610
 * @summary Compiler confused by parenthesized "this" in final fields assignments
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper

 * @run main T8204610
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class T8204610 extends ComboInstance<T8204610> {

    enum ParenKind implements ComboParameter {
        NONE(""),
        ONE("#P"),
        TWO("#P#P"),
        THREE("#P#P#P");

        String parensTemplate;

        ParenKind(String parensTemplate) {
            this.parensTemplate = parensTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return parensTemplate.replaceAll("#P", optParameter.equals("OPEN") ? "(" : ")");
        }
    }

    public static void main(String... args) {
        new ComboTestHelper<T8204610>()
                .withArrayDimension("PAREN", (x, pk, idx) -> x.parenKinds[idx] = pk, 3, ParenKind.values())
                .run(T8204610::new);
    }

    ParenKind[] parenKinds = new ParenKind[3];

    @Override
    public void doWork() {
        newCompilationTask()
                .withSourceFromTemplate(bodyTemplate)
                .analyze(this::check);
    }

    String bodyTemplate = "class Test {\n" +
                          "   final int x;\n" +
                          "   Test() {\n" +
                          "      #{PAREN[0].OPEN} #{PAREN[1].OPEN} this #{PAREN[1].CLOSE} . #{PAREN[2].OPEN} x #{PAREN[2].CLOSE} #{PAREN[0].CLOSE} = 1;\n" +
                          "   } }";

    void check(Result<?> res) {
        boolean expectedFail = parenKinds[2] != ParenKind.NONE;
        if (expectedFail != res.hasErrors()) {
            fail("unexpected compilation result for source:\n" +
                res.compilationInfo());
        }
    }
}
