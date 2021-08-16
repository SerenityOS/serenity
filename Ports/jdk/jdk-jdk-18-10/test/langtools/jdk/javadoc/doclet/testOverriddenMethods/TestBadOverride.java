/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8174839 8175200 8186332
 * @summary  Bad overriding method should not crash
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestBadOverride
 */

import javadoc.tester.JavadocTester;

public class TestBadOverride extends JavadocTester {

    /**
     * The entry point of the test.
     * @param args the array of command line arguments.
     */
    public static void main(String... args) throws Exception {
        TestBadOverride tester = new TestBadOverride();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg4");
        checkExit(Exit.OK);

        checkOutput("pkg4/Foo.html", true,
                """
                    <section class="detail" id="toString()">
                    <h3>toString</h3>
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type">void</span>&nbsp;<span class="element-name">toString</span>()</div>
                    <div class="block">Why can't I do this ?</div>
                    </section>""");
    }
}
