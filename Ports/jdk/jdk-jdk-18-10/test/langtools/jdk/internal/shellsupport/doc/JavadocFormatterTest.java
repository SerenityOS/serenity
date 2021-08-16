/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8131019 8169561 8261450
 * @summary Test JavadocFormatter
 * @library /tools/lib
 * @modules jdk.compiler/jdk.internal.shellsupport.doc
 * @run testng JavadocFormatterTest
 */

import java.util.Objects;

import jdk.internal.shellsupport.doc.JavadocFormatter;
import org.testng.annotations.Test;

@Test
public class JavadocFormatterTest {

    private static final String CODE_RESET = "\033[0m";
    private static final String CODE_HIGHLIGHT = "\033[1m";
    private static final String CODE_UNDERLINE = "\033[4m";

    public void testReflow() {
        String actual;
        String expected;

        actual = new JavadocFormatter(25, true).formatJavadoc(
                "test",
                "1234 1234\n1234\n1234 12345 123456789012345678901234567890 1234 1234\n1234 {@code 1234} 1234 1234\n1234 1234 123456 123456\n<b>123456</b>\n123456 123456 {@link String string} 1");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "1234 1234 1234 1234 12345\n" +
                   "123456789012345678901234567890\n" +
                   "1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 123456\n" +
                   "123456 123456 123456\n" +
                   "123456 string 1\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        actual = new JavadocFormatter(66, true).formatJavadoc("test",
                "@param <T> 51234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                "@param <E> 61234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                "@param shortName 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 \n" +
                "@param aVeryLongName1234567890123456789012345678901234567890 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 \n");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "\n" +
                   CODE_UNDERLINE + "Type Parameters:" + CODE_RESET + "\n" +
                   "T - 51234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "E - 61234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "\n" +
                   CODE_UNDERLINE + "Parameters:" + CODE_RESET + "\n" +
                   "shortName - 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234\n" +
                   "aVeryLongName1234567890123456789012345678901234567890 - \n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        actual = new JavadocFormatter(66, true).formatJavadoc("test",
                "@throws ShortExcp 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 \n" +
                "@throws aVeryLongException1234567890123456789012345678901234567890 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 \n");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "\n" +
                   CODE_UNDERLINE + "Thrown Exceptions:" + CODE_RESET + "\n" +
                   "ShortExcp - 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234\n" +
                   "aVeryLongException1234567890123456789012345678901234567890 - \n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }
        actual = new JavadocFormatter(66, true).formatJavadoc("test",
                "@return 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 \n");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "\n" +
                   CODE_UNDERLINE + "Returns:" + CODE_RESET + "\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 \n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //handling of <p>, <pre>:
        actual = new JavadocFormatter(66, true).formatJavadoc("test",
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 <p>1234 1234 <p>1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 <p>1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 \n" +
                "<blockquote><pre>\n" +
                "for (String data : content) {\n" +
                "    System.err.println(data);\n" +
                "}\n" +
                "</pre></blockquote>\n");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234\n" +
                   "1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234\n" +
                   "    for (String data : content) {\n" +
                   "        System.err.println(data);\n" +
                   "    }\n" +
                   "    \n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //list handling:
        actual = new JavadocFormatter(66, true).formatJavadoc("test",
                "<ul>" +
                "    <li>A 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</li>" +
                "    <li>B 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "    <li>C 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234<ol>" +
                "        <li>D 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</li>" +
                "        <li>E 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234<ul>" +
                "            <li>F 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234<ol>" +
                "                <li>G 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "            </ol>" +
                "        </ul>" +
                "    </OL>" +
                "    <LI><p>H 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 <p>1234 1234 1234 1234 1234 1234 1234<ul>" +
                "        <li>I 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "    </ul>" +
                "</ul> followup" +
                "<dl>" +
                "<dt>Term1</dt>" +
                "<dd>A 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</dd>" +
                "<dt>Term2" +
                "<dd>B 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "<dt>Term3" +
                "<dd>C 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "</dl>" +
                "<dl>" +
                "<dt>TermUnfinished" +
                "</dl> followup");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "  * A 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234\n" +
                   "  * B 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234\n" +
                   "  * C 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234\n" +
                   "     1. D 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "        1234 1234 1234 1234 1234 1234\n" +
                   "     2. E 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "        1234 1234 1234 1234 1234 1234\n" +
                   "          * F 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234\n" +
                   "             1. G 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "                1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "  * H 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234\n" +
                   "      * I 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "        1234 1234 1234 1234 1234 1234\n" +
                   "followup\n" +
                   CODE_HIGHLIGHT + "Term1" + CODE_RESET + "\n" +
                   "    A 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   CODE_HIGHLIGHT + "Term2" + CODE_RESET + "\n" +
                   "    B 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   CODE_HIGHLIGHT + "Term3" + CODE_RESET + "\n" +
                   "    C 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   CODE_HIGHLIGHT + "TermUnfinished" + CODE_RESET + "\n" +
                   "followup\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //sections:
        actual = new JavadocFormatter(66, true).formatJavadoc("test",
                "text 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "<h3>1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</h3>" +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "text 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "\n" +
                   CODE_UNDERLINE + "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234" + CODE_RESET + "\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "1234 1234 1234 1234 1234 1234 1234 1234\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //table:
        actual = new JavadocFormatter(66, true).formatJavadoc("test",
                "<table>" +
                "<tr>" +
                "<th>A 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</th>" +
                "<th>B 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</th>" +
                "<th>C 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</th>" +
                "</tr>" +
                "<tr>" +
                "<td>A 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</td>  \n" +
                "<td>B 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "<td>C 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "<tr>" +
                "<td>A 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</td>" +
                "<td>B 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</td>" +
                "<td>C 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</td>" +
                "</tr>" +
                "<tr>" +
                "<td>1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</td>" +
                "<td>1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234</td>" +
                "</table>");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "----------------------------------------------------------------\n" +
                   "| " + CODE_HIGHLIGHT + "A 1234 1234 1234" + CODE_RESET + "   | " + CODE_HIGHLIGHT + "B 1234 1234 1234" + CODE_RESET + "   | " + CODE_HIGHLIGHT + "C 1234 1234 1234" + CODE_RESET + "   |\n" +
                   "| " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     | " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     | " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     |\n" +
                   "| " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     | " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     | " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     |\n" +
                   "| " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     | " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     | " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     |\n" +
                   "| " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     | " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     | " + CODE_HIGHLIGHT + "1234 1234 1234" + CODE_RESET + "     |\n" +
                   "| " + CODE_HIGHLIGHT + "1234 1234" + CODE_RESET + "          | " + CODE_HIGHLIGHT + "1234 1234" + CODE_RESET + "          | " + CODE_HIGHLIGHT + "1234 1234" + CODE_RESET + "          |\n" +
                   "----------------------------------------------------------------\n" +
                   "| A 1234 1234 1234   | B 1234 1234 1234   | C 1234 1234 1234   |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234          | 1234 1234          | 1234 1234          |\n" +
                   "----------------------------------------------------------------\n" +
                   "| A 1234 1234 1234   | B 1234 1234 1234   | C 1234 1234 1234   |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234          | 1234 1234          | 1234 1234          |\n" +
                   "----------------------------------------------------------------\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234 1234     | 1234 1234 1234     |\n" +
                   "| 1234 1234          | 1234 1234          |\n" +
                   "-------------------------------------------\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //no escape sequences:
        actual = new JavadocFormatter(66, false).formatJavadoc("test",
                "@param shortName 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 \n" +
                "@param aVeryLongName1234567890123456789012345678901234567890 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 " +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234" +
                "                 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 \n");

        expected = "test\n" +
                   "\n" +
                   "Parameters:\n" +
                   "shortName - 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "            1234 1234 1234 1234\n" +
                   "aVeryLongName1234567890123456789012345678901234567890 - \n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n" +
                   "    1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234 1234\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //null javadoc:
        actual = new JavadocFormatter(66, true).formatJavadoc("test", null);

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //stray tags:
        for (String tag : new String[] {"li", "ol", "h3", "table", "tr", "td", "dl", "dt", "dd"}) {
            for (boolean closing : new boolean[] {false, true}) {
                actual = new JavadocFormatter(66, true).formatJavadoc("test",
                        "<" + (closing ? "/" : "") + tag + ">text");

                if (!actual.contains("text")) {
                    throw new AssertionError("Incorrect output: " + actual);
                }
            }
        }

        //entities:
        actual = new JavadocFormatter(66, false).formatJavadoc("test",
                "&alpha; &lt; &#65; &#X42; &gt; &broken; &#xFFFFFFFF; &#xFFFFFFF;\n");

        expected = "test\n" +
                   "\u03b1 < A B > &broken; &#xFFFFFFFF; &#xFFFFFFF;\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //img:
        actual = new JavadocFormatter(66, true).formatJavadoc("test",
                "1234 <img src='any.png' alt='text'/> 1234");

        expected = CODE_HIGHLIGHT + "test" + CODE_RESET + "\n" +
                   "1234 text 1234\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }

        //unknown HTML tag:
        actual = new JavadocFormatter(66, false).formatJavadoc("test",
                "1234 <unknown any any>1234</unknown> 1234");

        expected = "test\n" +
                   "1234 1234 1234\n";

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Incorrect output: " + actual);
        }
    }

    public void testSpaceAtEndOfLine() {
        String header = "Class<?> Class<T>.forName(Module module, String name)";
        String javadoc = """
                         @throws SecurityException
                                 <ul>
                                 <li> test </li>
                                 </ul>
                         """;

            new JavadocFormatter(60, true).formatJavadoc(header, javadoc);
    }

}
