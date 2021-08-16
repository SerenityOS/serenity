/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8267126 8267176
 * @summary  javadoc should show "line and caret" for diagnostics
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.* MyTaglet
 * @run main TestDiagsLineCaret
 */

import java.io.IOException;
import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestDiagsLineCaret extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDiagsLineCaret tester = new TestDiagsLineCaret();
        tester.runTests();
    }

    ToolBox tb = new ToolBox();

    @Test
    public void testDiags() throws IOException {
        tb.writeJavaFiles(Path.of("."), """
                /**
                 * First sentence.
                 * @since def &#x1f955; ghi
                 */
                public class MyClass { }
                """);

        String testClasses = System.getProperty("test.classes");

        javadoc("-d", "out",
                "-XDaccessInternalAPI",
                "-tagletpath", testClasses,
                "-taglet", "MyTaglet",
                "MyClass.java");
        checkExit(Exit.ERROR);

        checkOutput(Output.OUT, true,
                """
                    error: This is a error
                    warning: This is a warning
                    warning: This is a mandatory_warning
                    Note: This is a note
                    MyClass.java:5: error: This is a error for MyClass
                    public class MyClass { }
                           ^
                    MyClass.java:5: warning: This is a warning for MyClass
                    public class MyClass { }
                           ^
                    MyClass.java:5: warning: This is a mandatory_warning for MyClass
                    public class MyClass { }
                           ^
                    MyClass.java:5: Note: This is a note for MyClass
                    public class MyClass { }
                           ^
                    MyClass.java:3: error: This is a error: this is not a caret
                     * @since def &#x1f955; ghi
                                  ^
                    MyClass.java:3: warning: This is a warning: this is not a caret
                     * @since def &#x1f955; ghi
                                  ^
                    MyClass.java:3: warning: This is a mandatory_warning: this is not a caret
                     * @since def &#x1f955; ghi
                                  ^
                    MyClass.java:3: Note: This is a note: this is not a caret
                     * @since def &#x1f955; ghi
                                  ^
                    """,
                """
                    3 errors
                    6 warnings
                    """);
    }
}
