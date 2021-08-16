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
 * @bug 8251200
 * @summary False positive messages about missing comments for serialization
 * @library /tools/lib ../../lib/
 * @modules jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestSerialMissing
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestSerialMissing extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestSerialMissing tester = new TestSerialMissing();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) } );
    }

    ToolBox tb = new ToolBox();

    @Test
    public void testPackagePrivate(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    /** Module m. */
                    module m { exports p; }
                    """,
                """
                    package p;
                    /** PUBLIC class. */
                    public class PUBLIC extends Exception { }
                    """,
                """
                    package p;
                    // no comment: class should not be documented
                    class PACKAGE_PRIVATE extends Exception { }
                    """);

        javadoc("-d", base.resolve("api").toString(),
                "-sourcepath", src.toString(),
                "--module", "m");
        checkExit(Exit.OK);

        // should not be any reference to PACKAGE_PRIVATE.java, such as for no comment
        checkOutput(Output.OUT, false,
                "PACKAGE_PRIVATE");
    }
}