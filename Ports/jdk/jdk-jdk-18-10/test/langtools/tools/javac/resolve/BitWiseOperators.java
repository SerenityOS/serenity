/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8082311 8129962
 * @summary Verify that bitwise operators don't allow to mix numeric and boolean operands.
 * @library ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main BitWiseOperators
 */

import com.sun.tools.javac.util.StringUtils;

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;


public class BitWiseOperators extends ComboInstance<BitWiseOperators> {

    enum OperandType implements ComboParameter {
        BYTE,
        CHAR,
        SHORT,
        INT,
        LONG,
        BOOLEAN;

        public static boolean compatible(OperandType op1, OperandType op2) {
            return !(op1 == BOOLEAN ^ op2 == BOOLEAN);
        }

        @Override
        public String expand(String optParameter) {
            return StringUtils.toLowerCase(name());
        }
    }

    enum OperatorKind implements ComboParameter {
        BITAND("&"),
        BITOR("|"),
        BITXOR("^");

        String op;

        OperatorKind(String op) {
            this.op = op;
        }

        @Override
        public String expand(String optParameter) {
            return op;
        }
    }

    public static void main(String... args) {
        new ComboTestHelper<BitWiseOperators>()
                .withArrayDimension("TYPE", (x, type, idx) -> x.opTypes[idx] = type, 2, OperandType.values())
                .withDimension("OP", OperatorKind.values())
                .run(BitWiseOperators::new);
    }

    OperandType[] opTypes = new OperandType[2];

    String template = "class Test {\n" +
                      "    public Object test(#{TYPE[0]} var1, #{TYPE[1]} var2) {\n" +
                      "        return var1 #{OP} var2;\n" +
                      "    }\n" +
                      "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate(template)
                .analyze(res -> {
            if (res.hasErrors() == OperandType.compatible(opTypes[0], opTypes[1])) {
                fail("Unexpected behavior. Type1: " + opTypes[0] +
                        "; type2: " + opTypes[1] +
                        "; " + res.compilationInfo());
            }
        });
    }
}
