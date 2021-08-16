/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8137167
 * @summary Tests directive json parser
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @run driver compiler.compilercontrol.parser.DirectiveParserTest
 */

package compiler.compilercontrol.parser;

import compiler.compilercontrol.share.JSONFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;

public class DirectiveParserTest {
    private static final String ERROR_MSG = "VM should exit with error "
            + "on incorrect JSON file: ";
    private static final String EXPECTED_ERROR_STRING = "Parsing of compiler"
            + " directives failed";

    public static void main(String[] args) {
        simpleTest();
        nonMatchingBrackets();
        arrayTest();
        emptyObjectTest();
        emptyFile();
        noFile();
        directory();
    }

    private static void simpleTest() {
        String fileName = "simple.json";
        try (JSONFile file = new JSONFile(fileName)) {
            file.write(JSONFile.Element.ARRAY)
                    .write(JSONFile.Element.OBJECT)
                        .write(JSONFile.Element.PAIR, "match")
                        .write(JSONFile.Element.VALUE, "\"java/lang/String.*\"")
                        .write(JSONFile.Element.PAIR, "c2")
                        .write(JSONFile.Element.OBJECT)
                            .write(JSONFile.Element.PAIR, "inline")
                            .write(JSONFile.Element.ARRAY)
                                .write(JSONFile.Element.VALUE, "\"+*.indexOf\"")
                                .write(JSONFile.Element.VALUE, "\"-a.b\"")
                            .end()
                        .end()
                    .end() // end object
                    .write(JSONFile.Element.OBJECT)
                        .write(JSONFile.Element.PAIR, "match")
                        .write(JSONFile.Element.VALUE, "\"*.indexOf\"")
                        .write(JSONFile.Element.PAIR, "c1")
                        .write(JSONFile.Element.OBJECT)
                            .write(JSONFile.Element.PAIR, "enable")
                            .write(JSONFile.Element.VALUE, "false")
                        .end()
                    .end(); // end object
            file.end();
        }
        OutputAnalyzer output = HugeDirectiveUtil.execute(fileName);
        output.shouldHaveExitValue(0);
        output.shouldNotContain(EXPECTED_ERROR_STRING);
    }

    private static void nonMatchingBrackets() {
        String fileName = "non-matching.json";
        try (JSONFile file = new JSONFile(fileName)) {
            file.write(JSONFile.Element.ARRAY)
                    .write(JSONFile.Element.OBJECT)
                        .write(JSONFile.Element.PAIR, "match")
                        .write(JSONFile.Element.VALUE, "\"java/lang/String.*\"")
                    .end();
            // don't write matching }
        }
        OutputAnalyzer output = HugeDirectiveUtil.execute(fileName);
        Asserts.assertNE(output.getExitValue(), 0, ERROR_MSG + "non matching "
                + "brackets");
        output.shouldContain(EXPECTED_ERROR_STRING);
    }

    private static void arrayTest() {
        String fileName = "array.json";
        try (JSONFile file = new JSONFile(fileName)) {
            file.write(JSONFile.Element.ARRAY);
            file.end();
        }
        OutputAnalyzer output = HugeDirectiveUtil.execute(fileName);
        Asserts.assertNE(output.getExitValue(), 0, ERROR_MSG + "empty array");
    }

    private static void emptyObjectTest() {
        String fileName = "emptyObject.json";
        try (JSONFile file = new JSONFile(fileName)) {
            file.write(JSONFile.Element.OBJECT);
            file.end();
        }
        OutputAnalyzer output = HugeDirectiveUtil.execute(fileName);
        Asserts.assertNE(output.getExitValue(), 0, ERROR_MSG + "empty object "
                + "without any match");
        output.shouldContain(EXPECTED_ERROR_STRING);
    }

    private static void emptyFile() {
        String fileName = "empty.json";
        try (JSONFile file = new JSONFile(fileName)) {
            // empty
        }
        OutputAnalyzer output = HugeDirectiveUtil.execute(fileName);
        Asserts.assertNE(output.getExitValue(), 0, ERROR_MSG + "empty file");
        output.shouldContain(EXPECTED_ERROR_STRING);
    }

    private static void noFile() {
        OutputAnalyzer output = HugeDirectiveUtil.execute("nonexistent.json");
        Asserts.assertNE(output.getExitValue(), 0, ERROR_MSG + "non existing "
                + "file");
    }

    private static void directory() {
        OutputAnalyzer output = HugeDirectiveUtil.execute(Utils.TEST_SRC);
        Asserts.assertNE(output.getExitValue(), 0, ERROR_MSG + "directory as "
                + "a name");
    }
}
