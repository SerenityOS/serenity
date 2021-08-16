/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8137167
 * @summary Stress directive json parser
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @run driver compiler.compilercontrol.parser.DirectiveStressTest
 */

package compiler.compilercontrol.parser;

import compiler.compilercontrol.share.AbstractTestBase;
import compiler.compilercontrol.share.JSONFile;
import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.pool.PoolHelper;
import compiler.compilercontrol.share.scenario.DirectiveWriter;
import jdk.test.lib.process.OutputAnalyzer;

import java.util.List;
import java.util.stream.Collectors;

public class DirectiveStressTest {
    private static final int AMOUNT = Integer.getInteger(
            "compiler.compilercontrol.parser.DirectiveStressTest.amount",
            999);
    private static final List<MethodDescriptor> DESCRIPTORS
            = new PoolHelper().getAllMethods().stream()
                    .map(pair -> AbstractTestBase.getValidMethodDescriptor(
                            pair.first))
                    .collect(Collectors.toList());
    private static final String EXPECTED_MESSAGE = " compiler directives added";

    public static void main(String[] args) {
        hugeFileTest();
        hugeObjectTest();
    }

    /*
     * Creates file with AMOUNT of options in match block
     */
    private static void hugeObjectTest() {
        String fileName = "hugeObject.json";
        try (DirectiveWriter file = new DirectiveWriter(fileName)) {
            file.write(JSONFile.Element.ARRAY);
            HugeDirectiveUtil.createMatchObject(DESCRIPTORS, file, AMOUNT);
            file.end(); // end array block
        }
        OutputAnalyzer output = HugeDirectiveUtil.execute(fileName);
        output.shouldHaveExitValue(0);
        output.shouldContain(1 + EXPECTED_MESSAGE);
        output.shouldNotContain(HugeDirectiveUtil.EXPECTED_ERROR_STRING);
    }

    /*
     * Creates huge valid file with AMOUNT of match directives
     */
    private static void hugeFileTest() {
        String fileName = "hugeFile.json";
        HugeDirectiveUtil.createHugeFile(DESCRIPTORS, fileName, AMOUNT);
        OutputAnalyzer output = HugeDirectiveUtil.execute(fileName);
        output.shouldHaveExitValue(0);
        output.shouldContain(AMOUNT + EXPECTED_MESSAGE);
        output.shouldNotContain(HugeDirectiveUtil.EXPECTED_ERROR_STRING);
    }
}
