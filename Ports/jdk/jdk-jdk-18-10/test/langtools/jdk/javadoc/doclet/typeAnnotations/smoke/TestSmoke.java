/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006735
 * @summary  Smoke test for ensuring that annotations are emitted to javadoc
 *
 * @library  ../../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestSmoke
 */

import javadoc.tester.JavadocTester;

public class TestSmoke extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestSmoke tester = new TestSmoke();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-private",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/T0x1C.html", true, "@DA");
        checkOutput("pkg/T0x1D.html", true, "@DA");
        checkOutput("pkg/T0x0D.html", true, "@DA");
        checkOutput("pkg/T0x0B.html", true, "@DA");
        checkOutput("pkg/T0x0F.html", true, "@DA");
        checkOutput("pkg/T0x20B.html", true, "@DA");
        checkOutput("pkg/T0x22B.html", true, "@DA");
        checkOutput("pkg/T0x10.html", true, "@DA");
        checkOutput("pkg/T0x10A.html", true, "@DA");
        checkOutput("pkg/T0x11.html", true, "@DA");
        checkOutput("pkg/T0x12.html", true, "@DA");
        checkOutput("pkg/T0x13.html", true, "@DA");
        checkOutput("pkg/T0x14.html", true, "@DA");
        checkOutput("pkg/T0x15.html", true, "@DA");
        checkOutput("pkg/T0x16.html", true, "@DA");

        checkOutput("pkg/T0x06.html", true, "@DA");
        checkOutput("pkg/T0x20.html", true, "@DA");
        checkOutput("pkg/T0x20A.html", true, "@DTPA");
        checkOutput("pkg/T0x22.html", true, "@DA");
        checkOutput("pkg/T0x22A.html", true, "@DTPA");

        checkOutput("pkg/T0x1C.html", false, "@A");
        checkOutput("pkg/T0x1D.html", false, "@A");
        checkOutput("pkg/T0x00.html", false, "@A");
        checkOutput("pkg/T0x01.html", false, "@A");
        checkOutput("pkg/T0x02.html", false, "@A");
        checkOutput("pkg/T0x04.html", false, "@A");
        checkOutput("pkg/T0x08.html", false, "@A");
        checkOutput("pkg/T0x0D.html", false, "@A");
        checkOutput("pkg/T0x06.html", false, "@A");
        checkOutput("pkg/T0x0B.html", false, "@A");
        checkOutput("pkg/T0x0F.html", false, "@A");
        checkOutput("pkg/T0x20.html", false, "@A");
        checkOutput("pkg/T0x20A.html", false, "@A");
        checkOutput("pkg/T0x20B.html", false, "@A");
        checkOutput("pkg/T0x22.html", false, "@A");
        checkOutput("pkg/T0x22A.html", false, "@A");
        checkOutput("pkg/T0x22B.html", false, "@A");
        checkOutput("pkg/T0x10.html", false, "@A");
        checkOutput("pkg/T0x10A.html", false, "@A");
        checkOutput("pkg/T0x12.html", false, "@A");
        checkOutput("pkg/T0x11.html", false, "@A");
        checkOutput("pkg/T0x13.html", false, "@A");
        checkOutput("pkg/T0x15.html", false, "@A");
        checkOutput("pkg/T0x14.html", false, "@A");
        checkOutput("pkg/T0x16.html", false, "@A");
    }
}
