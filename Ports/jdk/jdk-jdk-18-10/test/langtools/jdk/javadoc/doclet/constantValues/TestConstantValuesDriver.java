/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4504730 4526070 5077317 8162363
 * @summary Test the generation of constant-values.html.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestConstantValuesDriver
 */
import javadoc.tester.JavadocTester;

public class TestConstantValuesDriver extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestConstantValuesDriver tester = new TestConstantValuesDriver();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                testSrc("TestConstantValues.java"),
                testSrc("TestConstantValues2.java"),
                testSrc("A.java"));
        checkExit(Exit.OK);

        checkOutput("constant-values.html", true,
                "TEST1PASSES",
                "TEST2PASSES",
                "TEST3PASSES",
                "TEST4PASSES",
                """
                    <code>"&lt;Hello World&gt;"</code>""",
                """
                    <code id="TestConstantValues.BYTE_MAX_VALUE">public&nbsp;static&nbsp;final&nbsp;byte</code></div>
                    <div class="col-second even-row-color"><code><a href="TestConstantValues.html#BYTE_MAX_VALUE">BYTE_MAX_VALUE</a></code></div>
                    <div class="col-last even-row-color"><code>0x7f</code></div>""",
                """
                    <code id="TestConstantValues.BYTE_MIN_VALUE">public&nbsp;static&nbsp;final&nbsp;byte</code></div>
                    <div class="col-second odd-row-color"><code><a href="TestConstantValues.html#BYTE_MIN_VALUE">BYTE_MIN_VALUE</a></code></div>
                    <div class="col-last odd-row-color"><code>0x81</code></div>""",
                """
                    <code id="TestConstantValues.CHAR_MAX_VALUE">public&nbsp;static&nbsp;final&nbsp;char</code></div>
                    <div class="col-second even-row-color"><code><a href="TestConstantValues.html#CHAR_MAX_VALUE">CHAR_MAX_VALUE</a></code></div>
                    <div class="col-last even-row-color"><code>'\\uffff'</code></div>""",
                """
                    <code id="TestConstantValues.DOUBLE_MAX_VALUE">public&nbsp;static&nbsp;final&nbsp;double</code></div>""",
                """
                    <div class="col-second odd-row-color"><code><a href="TestConstantValues.html#DOUBLE_MAX_VALUE">\
                    DOUBLE_MAX_VALUE</a></code></div>
                    <div class="col-last odd-row-color"><code>1.7976931348623157E308</code></div>""",
                """
                    <code id="TestConstantValues.DOUBLE_MIN_VALUE">public&nbsp;static&nbsp;final&nbsp;double</code></div>
                    <div class="col-second even-row-color"><code><a href="TestConstantValues.html#DOUBLE_MIN_VALUE">\
                    DOUBLE_MIN_VALUE</a></code></div>""",
                """
                    <code id="TestConstantValues.GOODBYE">public&nbsp;static&nbsp;final&nbsp;boolean</code></div>
                    <div class="col-second odd-row-color"><code><a href="TestConstantValues.html#GOODBYE">GOODBYE</a></code></div>""",
                """
                    <code id="TestConstantValues.HELLO">public&nbsp;static&nbsp;final&nbsp;boolean</code></div>
                    <div class="col-second even-row-color"><code><a href="TestConstantValues.html#HELLO">HELLO</a></code></div>
                    <div class="col-last even-row-color"><code>true</code></div>"""
        );
    }
}
