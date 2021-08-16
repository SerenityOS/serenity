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
 * @bug 8016110 8170832
 * @summary verify Japanese character in an argument are treated correctly
 * @compile -XDignore.symbol.file I18NArgTest.java
 * @run main I18NArgTest
 */
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class I18NArgTest extends TestHelper {
    public static void main(String... args) throws IOException {
        if (!isWindows) {
            return;
        }
        if (!"MS932".equals(System.getProperty("sun.jnu.encoding"))) {
            System.err.println("MS932 encoding not set, test skipped");
            return;
        }
        if (args.length == 0) {
            execTest(0x30bd); // MS932 Katakana SO, 0x835C
        } else {
            testCharacters(args);
        }
    }
    static void execTest(int unicodeValue) {
        String hexValue = Integer.toHexString(unicodeValue);
        String unicodeStr = Character.toString((char)unicodeValue);
        execTest("\"" + unicodeStr + "\"", hexValue);
        execTest("\\" + unicodeStr + "\\", hexValue);
        execTest(" " + unicodeStr + " ", hexValue);
        execTest("'" + unicodeStr + "'", hexValue);
        execTest("\t" + unicodeStr + "\t", hexValue);
        execTest("*" + unicodeStr + "*", hexValue);
        execTest("?" + unicodeStr + "?", hexValue);

        execTest("\"" + unicodeStr + unicodeStr + "\"", hexValue + hexValue);
        execTest("\\" + unicodeStr + unicodeStr + "\\", hexValue + hexValue);
        execTest(" " + unicodeStr + unicodeStr + " ", hexValue + hexValue);
        execTest("'" + unicodeStr + unicodeStr + "'", hexValue + hexValue);
        execTest("\t" + unicodeStr + unicodeStr + "\t", hexValue + hexValue);
        execTest("*" + unicodeStr + unicodeStr + "*", hexValue + hexValue);
        execTest("?" + unicodeStr + unicodeStr + "?", hexValue + hexValue);

        execTest("\"" + unicodeStr + "a" + unicodeStr + "\"", hexValue + "61" + hexValue);
        execTest("\\" + unicodeStr + "a" + unicodeStr + "\\", hexValue + "61" + hexValue);
        execTest(" " + unicodeStr + "a" + unicodeStr + " ", hexValue + "61"+ hexValue);
        execTest("'" + unicodeStr + "a" + unicodeStr + "'", hexValue + "61"+ hexValue);
        execTest("\t" + unicodeStr + "a" + unicodeStr + "\t", hexValue + "61"+ hexValue);
        execTest("*" + unicodeStr + "a" + unicodeStr + "*", hexValue + "61"+ hexValue);
        execTest("?" + unicodeStr + "a" + unicodeStr + "?", hexValue + "61"+ hexValue);

        execTest("\"" + unicodeStr + "\u00b1" + unicodeStr + "\"", hexValue + "b1" + hexValue);
        execTest("\\" + unicodeStr + "\u00b1" + unicodeStr + "\\", hexValue + "b1" + hexValue);
        execTest(" " + unicodeStr + "\u00b1" + unicodeStr + " ", hexValue + "b1"+ hexValue);
        execTest("'" + unicodeStr + "\u00b1" + unicodeStr + "'", hexValue + "b1"+ hexValue);
        execTest("\t" + unicodeStr + "\u00b1" + unicodeStr + "\t", hexValue + "b1"+ hexValue);
        execTest("*" + unicodeStr + "\u00b1" + unicodeStr + "*", hexValue + "b1"+ hexValue);
        execTest("?" + unicodeStr + "\u00b1" + unicodeStr + "?", hexValue + "b1"+ hexValue);
    }

    static void execTest(String unicodeStr, String hexValue) {
        TestResult tr = doExec(javaCmd,
                "-Dtest.src=" + TEST_SOURCES_DIR.getAbsolutePath(),
                "-Dtest.classes=" + TEST_CLASSES_DIR.getAbsolutePath(),
                "-cp", TEST_CLASSES_DIR.getAbsolutePath(),
                "I18NArgTest", unicodeStr, hexValue);
        System.out.println(tr.testOutput);
        if (!tr.isOK()) {
            System.err.println(tr);
            throw new RuntimeException("test fails");
        }

        // Test via JDK_JAVA_OPTIONS
        Map<String, String> env = new HashMap<>();
        String cmd = "-Dtest.src=" + TEST_SOURCES_DIR.getAbsolutePath() +
                " -Dtest.classes=" + TEST_CLASSES_DIR.getAbsolutePath() +
                " -cp " + TEST_CLASSES_DIR.getAbsolutePath() +
                " I18NArgTest " + unicodeStr + " " + hexValue;
        env.put("JDK_JAVA_OPTIONS", cmd);
        tr = doExec(env, javaCmd);
        System.out.println(tr.testOutput);
        if (!tr.isOK()) {
            System.err.println(tr);
            throw new RuntimeException("test fails");
        }
    }

    static void testCharacters(String... args) {
        String input = args[0];
        String expected = args[1];
        String hexValue = "";
        for (int i = 0; i < input.length(); i++) {
            hexValue = hexValue.concat(Integer.toHexString((int)input.charAt(i)));
        }
        System.out.println("input:" + input);
        System.out.println("expected:" + expected);
        System.out.println("obtained:" + hexValue);
        if (!hexValue.contains(expected)) {
            String message = "Error: output does not contain expected value" +
                "expected:" + expected + " obtained:" + hexValue;
            throw new RuntimeException(message);
        }
    }
}
