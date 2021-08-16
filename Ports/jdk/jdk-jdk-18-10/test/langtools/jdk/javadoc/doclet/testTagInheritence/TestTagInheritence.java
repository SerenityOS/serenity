/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4496223 4496270 4618686 4720974 4812240 6253614 6253604
 * @summary <DESC>
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestTagInheritence
 */

// TODO: Inheritence should be Inheritance!   fix separately as noreg-trivial
import javadoc.tester.JavadocTester;

public class TestTagInheritence extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestTagInheritence tester = new TestTagInheritence();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-Xdoclint:none",
                "-d", "out",
                "-sourcepath", testSrc,
                "pkg", "firstSentence", "firstSentence2");
        checkExit(Exit.OK);

        //Test bad inheritDoc tag warning.
        checkOutput(Output.OUT, true,
                "warning: @inheritDoc used but testBadInheritDocTag() "
                + "does not override or implement any method.");

        //Test valid usage of inheritDoc tag.
        for (int i = 1; i < 40; i++) {
            checkOutput("pkg/TestTagInheritence.html", true,
                    "Test " + i + " passes");
        }

        //First sentence test (6253614)
        checkOutput("firstSentence/B.html", true,
                """
                    <div class="block">First sentence.</div>""");

        //Another first sentence test (6253604)
        checkOutput("firstSentence2/C.html", true,
                """
                    <div class="block">First sentence.</div>""");
    }
}
